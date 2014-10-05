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

import com.github.mauricio.async.db.QueryResult
import com.github.mauricio.async.db.column.{ColumnEncoderRegistry, ColumnDecoderRegistry}
import com.github.mauricio.async.db.exceptions.{InsufficientParametersException, ConnectionStillRunningQueryException}
import com.github.mauricio.async.db.general.MutableResultSet
import com.github.mauricio.async.db.postgresql.codec.{PostgreSQLConnectionDelegate, PostgreSQLConnectionHandler}
import com.github.mauricio.async.db.postgresql.column.{PostgreSQLColumnDecoderRegistry, PostgreSQLColumnEncoderRegistry}
import com.github.mauricio.async.db.postgresql.exceptions._
import com.github.mauricio.async.db.util._
import com.github.mauricio.async.db.{Configuration, Connection}
import java.util.concurrent.atomic.{AtomicLong,AtomicInteger,AtomicReference}
import messages.backend._
import messages.frontend._
import scala.Some
import scala.concurrent._
import io.netty.channel.EventLoopGroup
import java.util.concurrent.CopyOnWriteArrayList

object PostgreSQLConnection {
  final val Counter = new AtomicLong()
  final val ServerVersionKey = "server_version"
  final val log = Log.get[PostgreSQLConnection]
}

class PostgreSQLConnection
(
  configuration: Configuration = Configuration.Default,
  encoderRegistry: ColumnEncoderRegistry = PostgreSQLColumnEncoderRegistry.Instance,
  decoderRegistry: ColumnDecoderRegistry = PostgreSQLColumnDecoderRegistry.Instance,
  group : EventLoopGroup = NettyUtils.DefaultEventLoopGroup,
  executionContext : ExecutionContext = ExecutorServiceUtils.CachedExecutionContext
  )
  extends PostgreSQLConnectionDelegate
  with Connection {

  import PostgreSQLConnection._

  private final val connectionHandler = new PostgreSQLConnectionHandler(
    configuration,
    encoderRegistry,
    decoderRegistry,
    this,
    group,
    executionContext
  )

  private final val currentCount = Counter.incrementAndGet()
  private final val preparedStatementsCounter = new AtomicInteger()
  private final implicit val internalExecutionContext = executionContext

  private val parameterStatus = new scala.collection.mutable.HashMap[String, String]()
  private val parsedStatements = new scala.collection.mutable.HashMap[String, PreparedStatementHolder]()
  private var authenticated = false

  private val connectionFuture = Promise[Connection]()

  private var recentError = false
  private val queryPromiseReference = new AtomicReference[Option[Promise[QueryResult]]](None)
  private var currentQuery: Option[MutableResultSet[PostgreSQLColumnData]] = None
  private var currentPreparedStatement: Option[PreparedStatementHolder] = None
  private var version = Version(0,0,0)
  private var notifyListeners = new CopyOnWriteArrayList[NotificationResponse => Unit]()
  
  private var queryResult: Option[QueryResult] = None

  def isReadyForQuery: Boolean = this.queryPromise.isEmpty

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

    val promise = Promise[QueryResult]()
    this.setQueryPromise(promise)

    write(new QueryMessage(query))

    promise.future
  }

  override def sendPreparedStatement(query: String, values: Seq[Any] = List()): Future[QueryResult] = {
    validateQuery(query)

    val promise = Promise[QueryResult]()
    this.setQueryPromise(promise)

    val holder = this.parsedStatements.getOrElseUpdate(query,
      new PreparedStatementHolder( query, preparedStatementsCounter.incrementAndGet ))

    if (holder.paramsCount != values.length) {
      this.clearQueryPromise
      throw new InsufficientParametersException(holder.paramsCount, values)
    }

    this.currentPreparedStatement = Some(holder)
    this.currentQuery = Some(new MutableResultSet(holder.columnDatas))
    write(
      if (holder.prepared)
        new PreparedStatementExecuteMessage(holder.statementId, holder.realQuery, values, this.encoderRegistry)
      else {
        holder.prepared = true
        new PreparedStatementOpeningMessage(holder.statementId, holder.realQuery, values, this.encoderRegistry)
      })

    promise.future
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

    this.currentPreparedStatement.map(p => this.parsedStatements.remove(p.query))
    this.currentPreparedStatement = None
    this.failQueryPromise(e)
  }

  override def onReadyForQuery() {
    this.connectionFuture.trySuccess(this)
    
    this.recentError = false
    queryResult.foreach(this.succeedQueryPromise)
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
    if ( ServerVersionKey == m.key ) {
      this.version = Version(m.value)
    }
  }

  override def onDataRow(m: DataRowMessage) {
    val items = new Array[Any](m.values.size)
    var x = 0

    while ( x < m.values.size ) {
      items(x) = if ( m.values(x) == null ) {
        null
      } else {
        val columnType = this.currentQuery.get.columnTypes(x)
        this.decoderRegistry.decode(columnType, m.values(x), configuration.charset)
      }
      x += 1
    }

    this.currentQuery.get.addRow(items)
  }

  override def onRowDescription(m: RowDescriptionMessage) {
    this.currentQuery = Option(new MutableResultSet(m.columnDatas))
    this.setColumnDatas(m.columnDatas)
  }

  private def setColumnDatas( columnDatas : Array[PostgreSQLColumnData] ) {
    this.currentPreparedStatement.foreach { holder =>
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

  override def onNotificationResponse( message : NotificationResponse ) {
    val iterator = this.notifyListeners.iterator()
    while ( iterator.hasNext ) {
      iterator.next().apply(message)
    }
  }

  def registerNotifyListener( listener : NotificationResponse => Unit ) {
    this.notifyListeners.add(listener)
  }

  def unregisterNotifyListener( listener : NotificationResponse => Unit ) {
    this.notifyListeners.remove(listener)
  }

  def clearNotifyListeners() {
    this.notifyListeners.clear()
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

  private[this] def notReadyForQueryError(errorMessage : String, race : Boolean) = {
    log.error(errorMessage)
    throw new ConnectionStillRunningQueryException(
      this.currentCount,
      race
    )
  }
  
  def validateIfItIsReadyForQuery(errorMessage: String) =
    if (this.queryPromise.isDefined)
      notReadyForQueryError(errorMessage, false)
  
  private def validateQuery(query: String) {
    this.validateIfItIsReadyForQuery("Can't run query because there is one query pending already")

    if (query == null || query.isEmpty) {
      throw new QueryMustNotBeNullOrEmptyException(query)
    }
  }

  private def queryPromise: Option[Promise[QueryResult]] = queryPromiseReference.get()

  private def setQueryPromise(promise: Promise[QueryResult]) {
    if (!this.queryPromiseReference.compareAndSet(None, Some(promise)))
      notReadyForQueryError("Can't run query due to a race with another started query", true)
  }

  private def clearQueryPromise : Option[Promise[QueryResult]] = {
    this.queryPromiseReference.getAndSet(None)
  }

  private def failQueryPromise(t: Throwable) {
    this.clearQueryPromise.foreach { promise =>
      log.error("Setting error on future {}", promise)
      promise.failure(t)
    }
  }

  private def succeedQueryPromise(result: QueryResult) {
    this.queryResult = None
    this.currentQuery = None
    this.clearQueryPromise.foreach {
      _.success(result)
    }
  }

  private def write( message : ClientMessage ) {
    this.connectionHandler.write(message)
  }

  override def toString: String = {
    s"${this.getClass.getSimpleName}{counter=${this.currentCount}}"
  }
}
