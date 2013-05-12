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
import java.util.concurrent.atomic._
import messages.backend._
import messages.frontend._
import org.jboss.netty.logging.{Slf4JLoggerFactory, InternalLoggerFactory}
import scala.Some
import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.Some
import com.github.mauricio.async.db.postgresql.messages.backend.DataRowMessage
import com.github.mauricio.async.db.postgresql.messages.backend.CommandCompleteMessage
import com.github.mauricio.async.db.postgresql.messages.backend.RowDescriptionMessage
import com.github.mauricio.async.db.postgresql.messages.backend.ParameterStatusMessage
import com.github.mauricio.async.db.QueryResult

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
  private final val preparedStatementsCounter = new AtomicInteger()
  private final implicit val executionContext = ExecutionContext.fromExecutorService(configuration.workerPool)

  private var readyForQuery = false
  private val parameterStatus = new scala.collection.mutable.HashMap[String, String]()
  private val parsedStatements = new scala.collection.mutable.HashMap[String, PreparedStatementHolder]()
  private var authenticated = false

  private val connectionFuture = Promise[Connection]()

  private var recentError = false
  private val queryPromiseReference = new AtomicReference[Option[Promise[QueryResult]]](None)
  private var currentQuery: Option[MutableResultSet[PostgreSQLColumnData]] = None
  private var currentPreparedStatement: Option[String] = None
  
  private var queryResult: Option[QueryResult] = None

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
    this.currentPreparedStatement = Some(query)

    this.isParsed(query) match {
      case Some(holder) => {
        this.currentQuery = Some(new MutableResultSet(holder.columnDatas, configuration.charset, this.decoderRegistry))
        write(new PreparedStatementExecuteMessage(holder.statementId, realQuery, values, this.encoderRegistry))
      }
      case None => {
        val statementId = this.preparedStatementsCounter.incrementAndGet()
        this.parsedStatements.put( query, new PreparedStatementHolder( statementId ) )
        write(new PreparedStatementOpeningMessage(statementId, realQuery, values, this.encoderRegistry))
      }
    }

    promise.future
  }

  private def isParsed(query: String): Option[PreparedStatementHolder] = {
    this.parsedStatements.get(query)
  }

  override def onError( exception : Throwable ) {
    this.setErrorOnFutures(exception)
  }

  def hasRecentError: Boolean = this.recentError

  private def setErrorOnFutures(e: Throwable) {
    this.recentError = true

    log.error("Error on connection", e)

    if (!this.connectionFuture.isCompleted) {
      this.connectionFuture.failure(e)
      this.disconnect
    }

    this.failQueryPromise(e)

    this.currentPreparedStatement = None
  }

  override def onReadyForQuery() {
    this.connectionFuture.trySuccess(this)
    
    queryResult.map(this.succeedQueryPromise)
    
    this.queryResult = None
    this.recentError = false
    this.readyForQuery = true
  }

  override def onError(m: ErrorMessage) {
    log.error("Error with message -> {}", m)

    val error = new GenericDatabaseException(m)
    error.fillInStackTrace()

    this.setErrorOnFutures(error)
  }

  override def onCommandComplete(m: CommandCompleteMessage) {
    this.currentPreparedStatement = None
    queryResult = Some(new QueryResult(m.rowsAffected, m.statusMessage, this.currentQuery))
  }

  override def onParameterStatus(m: ParameterStatusMessage) {
    this.parameterStatus.put(m.key, m.value)
  }

  override def onDataRow(m: DataRowMessage) {
    this.currentQuery.get.addRawRow(m.values)
  }

  override def onRowDescription(m: RowDescriptionMessage) {
    this.currentQuery = Option(new MutableResultSet(m.columnDatas, configuration.charset, this.decoderRegistry))
    this.setColumnDatas(m.columnDatas)
  }

  private def setColumnDatas( columnDatas : Array[PostgreSQLColumnData] ) {
    if (this.currentPreparedStatement.isDefined) {
      val holder = this.parsedStatements(this.currentPreparedStatement.get)
      holder.columnDatas = columnDatas
    }
  }

  override def onAuthenticationResponse(message: AuthenticationMessage) {

    message match {
      case m: AuthenticationOkMessage => {
        log.debug("Successfully logged in to database")
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
      log.error(errorMessage)
      throw new ConnectionStillRunningQueryException(
        this.currentCount,
        this.readyForQuery
      )
    }
  
  private def validateQuery(query: String) {
    this.validateIfItIsReadyForQuery("Can't run query because there is one query pending already")

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
      log.error("Setting error on future {}", promise)
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
    s"${this.getClass.getSimpleName}{counter=${this.currentCount}}"
  }
}