/*
 * Copyright 2013 Maurício Linhares
 *
 * Maurício Linhares licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.github.mauricio.async.db.postgresql

import com.github.mauricio.async.db.column.{ColumnEncoderRegistry, ColumnDecoderRegistry}
import com.github.mauricio.async.db.general.MutableResultSet
import com.github.mauricio.async.db.postgresql.codec.{PostgreSQLConnectionDelegate, PostgreSQLConnectionHandler}
import com.github.mauricio.async.db.postgresql.column.{PostgreSQLColumnDecoderRegistry, PostgreSQLColumnEncoderRegistry}
import com.github.mauricio.async.db.postgresql.exceptions._
import com.github.mauricio.async.db.util.Log
import com.github.mauricio.async.db.{Configuration, QueryResult, Connection}
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.{AtomicReference, AtomicLong}
import messages.backend._
import messages.frontend._
import org.jboss.netty.logging.{Slf4JLoggerFactory, InternalLoggerFactory}
import scala.Some
import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future, Promise}

object PostgreSQLConnection {
  val log = Log.get[PostgreSQLConnection]
  val Name = "Netty-PostgreSQL-driver-0.1.2"
  InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory())
  val Counter = new AtomicLong()
}

class PostgreSQLConnection
(
  configuration: Configuration = Configuration.Default,
  encoderRegistry: ColumnEncoderRegistry = PostgreSQLColumnEncoderRegistry.Instance,
  decoderRegistry: ColumnDecoderRegistry = PostgreSQLColumnDecoderRegistry.Instance
  )
  extends PostgreSQLConnectionDelegate
  with Connection {

  import PostgreSQLConnection._

  private final val connectionHandler = new PostgreSQLConnectionHandler( configuration, encoderRegistry, decoderRegistry, this )
  private final val currentCount = Counter.incrementAndGet()
  private final implicit val executionContext = ExecutionContext.fromExecutorService(configuration.workerPool)

  private var readyForQuery = false
  private val parameterStatus = new ConcurrentHashMap[String, String]()
  private val parsedStatements = new ConcurrentHashMap[String, Array[PostgreSQLColumnData]]()
  private var authenticated = false

  private val connectionFuture = Promise[Connection]()

  private var recentError = false
  private val queryPromiseReference = new AtomicReference[Option[Promise[QueryResult]]](None)
  private var currentQuery: Option[MutableResultSet[PostgreSQLColumnData]] = None
  private var currentPreparedStatement: Option[String] = None

  def isReadyForQuery: Boolean = this.readyForQuery

  def connect: Future[Connection] = {
    this.connectionHandler.connect.onFailure {
      case e => this.connectionFuture.tryFailure(e)
    }

    this.connectionFuture.future
  }

  override def disconnect: Future[Connection] = this.connectionHandler.disconnect.map( c => this )

  override def isConnected: Boolean = this.connectionHandler.isConnected

  def parameterStatuses: scala.collection.immutable.Map[String, String] = this.parameterStatus.toMap

  override def sendQuery(query: String): Future[QueryResult] = {
    validateQuery(query)
    this.readyForQuery = false

    val promise = Promise[QueryResult]()
    this.setQueryPromise(promise)

    write(new QueryMessage(query))

    promise.future
  }

  override def sendPreparedStatement(query: String, values: Seq[Any] = List()): Future[QueryResult] = {
    validateQuery(query)

    var paramsCount = 0

    val realQuery = if (query.contains("?")) {
      query.foldLeft(new StringBuilder()) {
        (builder, char) =>
          if (char == '?') {
            paramsCount += 1
            builder.append("$" + paramsCount)
          } else {
            builder.append(char)
          }
          builder
      }.toString()
    } else {
      query
    }

    if (paramsCount != values.length) {
      throw new InsufficientParametersException(paramsCount, values)
    }

    this.readyForQuery = false
    val promise = Promise[QueryResult]()
    this.setQueryPromise(promise)
    this.currentPreparedStatement = Some(realQuery)

    if (!this.isParsed(realQuery)) {
      write(new PreparedStatementOpeningMessage(realQuery, values, this.encoderRegistry))
    } else {
      this.currentQuery = Some(new MutableResultSet(this.parsedStatements.get(realQuery), configuration.charset, this.decoderRegistry))
      write(new PreparedStatementExecuteMessage(realQuery, values, this.encoderRegistry))
    }

    promise.future
  }

  override def onError( exception : Throwable ) {
    this.setErrorOnFutures(exception)
  }

  def hasRecentError: Boolean = this.recentError

  private def setErrorOnFutures(e: Throwable) {
    this.recentError = true

    log.error("[%s] - Error on connection".format(currentCount), e)

    if (!this.connectionFuture.isCompleted) {
      this.connectionFuture.failure(e)
      this.disconnect
    }

    this.failQueryPromise(e)

    this.currentPreparedStatement = None
  }

  override def onReadyForQuery() {
    this.recentError = false
    this.readyForQuery = true
    this.clearQueryPromise

    this.connectionFuture.trySuccess(this)
  }

  override def onError(m: ErrorMessage) {
    log.error("[%s] - Error with message -> {}".format(currentCount), m)

    val error = new GenericDatabaseException(m)
    error.fillInStackTrace()

    this.setErrorOnFutures(error)
  }

  override def onCommandComplete(m: CommandCompleteMessage) {
    this.currentPreparedStatement = None
    this.succeedQueryPromise(new QueryResult(m.rowsAffected, m.statusMessage, this.currentQuery))
  }

  override def onParameterStatus(m: ParameterStatusMessage) {
    this.parameterStatus.put(m.key, m.value)
  }

  override def onDataRow(m: DataRowMessage) {
    this.currentQuery.get.addRawRow(m.values)
  }

  override def onParseComplete() {
    setColumnDatas(Array.empty)
  }

  override def onRowDescription(m: RowDescriptionMessage) {
    this.currentQuery = Option(new MutableResultSet(m.columnDatas, configuration.charset, this.decoderRegistry))
    this.setColumnDatas(m.columnDatas)
  }

  private def setColumnDatas( columnDatas : Array[PostgreSQLColumnData] ) {
    if (this.currentPreparedStatement.isDefined) {
      this.parsedStatements.put(this.currentPreparedStatement.get, columnDatas)
    }
  }

  private def isParsed(query: String): Boolean = {
    this.parsedStatements.containsKey(query)
  }

  override def onAuthenticationResponse(message: AuthenticationMessage) {

    message match {
      case m: AuthenticationOkMessage => {
        log.debug("[{}] - Successfully logged in to database", this.currentCount)
        this.authenticated = true
      }
      case m: AuthenticationChallengeCleartextMessage => {
        write(this.credential(m))
      }
      case m: AuthenticationChallengeMD5 => {
        write(this.credential(m))
      }
    }

  }

  private def credential(authenticationMessage: AuthenticationChallengeMessage): CredentialMessage = {
    if (configuration.username != null && configuration.password.isDefined) {
      new CredentialMessage(
        configuration.username,
        configuration.password.get,
        authenticationMessage.challengeType,
        authenticationMessage.salt
      )
    } else {
      throw new MissingCredentialInformationException(
        this.configuration.username,
        this.configuration.password,
        authenticationMessage.challengeType)
    }
  }
  
  def validateIfItIsReadyForQuery(errorMessage: String) = 
    if (this.queryPromise.isDefined) {
      log.error(errorMessage, this.currentCount)
      throw new ConnectionStillRunningQueryException(
        this.currentCount,
        this.readyForQuery
      )
    }
  
  private def validateQuery(query: String) {
    this.validateIfItIsReadyForQuery(
            errorMessage = "[{}] - Can't run query because there is one query pending already")

    if (query == null || query.isEmpty) {
      throw new QueryMustNotBeNullOrEmptyException(query)
    }
  }

  private def queryPromise: Option[Promise[QueryResult]] = queryPromiseReference.get()

  private def setQueryPromise(promise: Promise[QueryResult]) {
    this.queryPromiseReference.set(Some(promise))
  }

  private def clearQueryPromise {
    this.queryPromiseReference.set(None)
  }

  private def failQueryPromise(t: Throwable) {
    val promise = this.queryPromise

    if (promise.isDefined) {
      this.clearQueryPromise
      log.error("[{}] - Setting error on future {}", this.currentCount, promise)
      promise.get.failure(t)
    }
  }

  private def succeedQueryPromise(result: QueryResult) {
    val promise = this.queryPromise

    if (promise.isDefined) {
      this.clearQueryPromise
      promise.get.success(result)
    }
  }

  def write( message : ClientMessage ) {
    this.connectionHandler.write(message)
  }

  override def toString: String = {
    "%s{counter=%s}".format(this.getClass.getSimpleName, this.currentCount)
  }
}