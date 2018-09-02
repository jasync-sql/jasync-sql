package com.github.mauricio.async.db.postgresql

import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.column.ColumnDecoderRegistry
import com.github.jasync.sql.db.column.ColumnEncoderRegistry
import com.github.jasync.sql.db.pool.TimeoutScheduler
import com.github.jasync.sql.db.util.ExecutorServiceUtils
import com.github.jasync.sql.db.util.NettyUtils
import com.github.mauricio.async.db.QueryResult
import com.github.mauricio.async.db.column.ColumnDecoderRegistry
import com.github.mauricio.async.db.column.ColumnEncoderRegistry
import com.github.mauricio.async.db.exceptions.ConnectionStillRunningQueryException
import com.github.mauricio.async.db.exceptions.InsufficientParametersException
import com.github.mauricio.async.db.general.MutableResultSet
import com.github.mauricio.async.db.pool.TimeoutScheduler
import com.github.mauricio.async.db.postgresql.codec.PostgreSQLConnectionDelegate
import com.github.mauricio.async.db.postgresql.codec.PostgreSQLConnectionHandler
import com.github.mauricio.async.db.postgresql.column.PostgreSQLColumnDecoderRegistry
import com.github.mauricio.async.db.postgresql.column.PostgreSQLColumnEncoderRegistry
import com.github.mauricio.async.db.postgresql.exceptions.*
import com.github.mauricio.async.db.util.*
import com.github.mauricio.async.db.Configuration
import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.postgresql.util.DEFAULT
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

import messages.backend.*
import messages.frontend.*

import scala.concurrent.*
import io.netty.channel.EventLoopGroup
import java.util.concurrent.CopyOnWriteArrayList

import com.github.mauricio.async.db.postgresql.util.URLParser
import mu.KotlinLogging
import java.util.concurrent.ExecutorService

private val logger = KotlinLogging.logger {}

class PostgreSQLConnection
(
    configuration: Configuration = DEFAULT,
    encoderRegistry: ColumnEncoderRegistry = PostgreSQLColumnEncoderRegistry.Instance,
    decoderRegistry: ColumnDecoderRegistry = PostgreSQLColumnDecoderRegistry.Instance,
    group: EventLoopGroup = NettyUtils.DefaultEventLoopGroup,
    val executionContext: ExecutorService = ExecutorServiceUtils.CachedThreadPool
) : PostgreSQLConnectionDelegate, Connection, TimeoutScheduler {

  companion object {
    val Counter = AtomicLong()
    val ServerVersionKey = "server_version"
  }

  private val connectionHandler = PostgreSQLConnectionHandler(
      configuration,
      encoderRegistry,
      decoderRegistry,
      this,
      group,
      executionContext
  )

  private val currentCount = Counter.incrementAndGet()
  private val preparedStatementsCounter = AtomicInteger()

  private val parameterStatus = scala.collection.mutable.HashMap<String, String>()
  private val parsedStatements = scala.collection.mutable.HashMap<String, PreparedStatementHolder>()
  private var authenticated = false

  private val connectionFuture = CompletableFuture<Connection>()

  private var recentError = false
  private val queryPromiseReference = AtomicReference<Option<Promise<QueryResult>>>(None)
  private var currentQuery: Option<MutableResultSet<PostgreSQLColumnData>> = None
  private var currentPreparedStatement: Option<PreparedStatementHolder> = None
  private var version = Version(0, 0, 0)
  private var notifyListeners = CopyOnWriteArrayList < NotificationResponse -> Unit>()

  private var queryResult: Option<QueryResult> = None

  override fun eventLoopGroup(): EventLoopGroup = group
  fun isReadyForQuery: Boolean = this.queryPromise.isEmpty

  fun connect: CompletableFuture<Connection> {
    this.connectionHandler.connect.onFailure { e ->
      this.connectionFuture.tryFailure(e)
    }

    this.connectionFuture.future
  }

  override fun disconnect: CompletableFuture<Connection> = this.connectionHandler.disconnect.map(c -> this )
  override fun onTimeout() = disconnect

  override fun isConnected: Boolean = this.connectionHandler.isConnected

  fun parameterStatuses: scala.collection.immutable.Map<String, String> = this.parameterStatus.toMap

  override fun sendQuery(query: String): CompletableFuture<QueryResult> {
    validateQuery(query)

    val promise = CompletableFuture<QueryResult>()
    this.setQueryPromise(promise)

    write(QueryMessage(query))
    addTimeout(promise, configuration.queryTimeout)
    promise.future
  }

  override fun sendPreparedStatement(query: String, values: List<Any> = List()): CompletableFuture<QueryResult> {
    validateQuery(query)

    val promise = CompletableFuture<QueryResult>()
    this.setQueryPromise(promise)

    val holder = this.parsedStatements.getOrElseUpdate(query,
        PreparedStatementHolder(query, preparedStatementsCounter.incrementAndGet))

    if (holder.paramsCount != values.length) {
      this.clearQueryPromise
      throw InsufficientParametersException(holder.paramsCount, values)
    }

    this.currentPreparedStatement = Some(holder)
    this.currentQuery = Some(MutableResultSet(holder.columnDatas))
    write(
        if (holder.prepared)
          PreparedStatementExecuteMessage(holder.statementId, holder.realQuery, values, this.encoderRegistry)
        else {
          holder.prepared = true
          PreparedStatementOpeningMessage(holder.statementId, holder.realQuery, values, this.encoderRegistry)
        })
    addTimeout(promise, configuration.queryTimeout)
    promise.future
  }

  override fun onError(exception: Throwable) {
    this.setErrorOnFutures(exception)
  }

  fun hasRecentError: Boolean = this.recentError

  private fun setErrorOnFutures(e: Throwable) {
    this.recentError = true

    log.error("Error on connection", e)

    if (!this.connectionFuture.isCompleted) {
      this.connectionFuture.failure(e)
      this.disconnect
    }

    this.currentPreparedStatement.map(p -> this.parsedStatements.remove(p.query))
    this.currentPreparedStatement = None
    this.failQueryPromise(e)
  }

  override fun onReadyForQuery() {
    this.connectionFuture.trySuccess(this)

    this.recentError = false
    queryResult.foreach(this.succeedQueryPromise)
  }

  override fun onError(m: ErrorMessage) {
    log.error("Error , message -> {}", m)

    val error = GenericDatabaseException(m)
    error.fillInStackTrace()

    this.setErrorOnFutures(error)
  }

  override fun onCommandComplete(m: CommandCompleteMessage) {
    this.currentPreparedStatement = None
    queryResult = Some(QueryResult(m.rowsAffected, m.statusMessage, this.currentQuery))
  }

  override fun onParameterStatus(m: ParameterStatusMessage) {
    this.parameterStatus.put(m.key, m.value)
    if (ServerVersionKey == m.key) {
      this.version = Version(m.value)
    }
  }

  override fun onDataRow(m: DataRowMessage) {
    val items = Array<Any>(m.values.size)
    var x = 0

    while (x < m.values.size) {
      val buf = m.values(x)
      items(x) = if (buf == null) {
        null
      } else {
        try {
          val columnType = this.currentQuery.get.columnTypes(x)
          this.decoderRegistry.decode(columnType, buf, configuration.charset)
        } finally {
          buf.release()
        }
      }
      x += 1
    }

    this.currentQuery.get.addRow(items)
  }

  override fun onRowDescription(m: RowDescriptionMessage) {
    this.currentQuery = Option(MutableResultSet(m.columnDatas))
    this.setColumnDatas(m.columnDatas)
  }

  private fun setColumnDatas(columnDatas: Array<PostgreSQLColumnData>) {
    this.currentPreparedStatement.foreach { holder ->
      holder.columnDatas = columnDatas
    }
  }

  override fun onAuthenticationResponse(message: AuthenticationMessage) {

    message when {
      m: AuthenticationOkMessage
      -> {
        log.debug("Successfully logged in to database")
        this.authenticated = true
      }
      m: AuthenticationChallengeCleartextMessage
      -> {
        write(this.credential(m))
      }
      m: AuthenticationChallengeMD5
      -> {
        write(this.credential(m))
      }
    }

  }

  override fun onNotificationResponse(message: NotificationResponse) {
    val iterator = this.notifyListeners.iterator()
    while (iterator.hasNext) {
      iterator.next().apply(message)
    }
  }

  fun registerNotifyListener(listener: NotificationResponse -> Unit )
  {
    this.notifyListeners.add(listener)
  }

  fun unregisterNotifyListener(listener: NotificationResponse -> Unit )
  {
    this.notifyListeners.remove(listener)
  }

  fun clearNotifyListeners() {
    this.notifyListeners.clear()
  }

  private fun credential(authenticationMessage: AuthenticationChallengeMessage): CredentialMessage {
    if (configuration.username != null && configuration.password.isDefined) {
      CredentialMessage(
          configuration.username,
          configuration.password.get,
          authenticationMessage.challengeType,
          authenticationMessage.salt
      )
    } else {
      throw MissingCredentialInformationException(
          this.configuration.username,
          this.configuration.password,
          authenticationMessage.challengeType)
    }
  }

  private <this>
  fun notReadyForQueryError(errorMessage: String, race: Boolean) {
    log.error(errorMessage)
    throw ConnectionStillRunningQueryException(
        this.currentCount,
        race
    )
  }

  fun validateIfItIsReadyForQuery(errorMessage: String) =
      if (this.queryPromise.isDefined)
        notReadyForQueryError(errorMessage, false)

  private fun validateQuery(query: String) {
    this.validateIfItIsReadyForQuery("Can't run query because there is one query pending already")

    if (query == null || query.isEmpty) {
      throw QueryMustNotBeNullOrEmptyException(query)
    }
  }

  private fun queryPromise: Option<Promise<QueryResult>> = queryPromiseReference.get()

  private fun setQueryPromise(promise: CompletableFuture<QueryResult>) {
    if (!this.queryPromiseReference.compareAndSet(None, Some(promise)))
      notReadyForQueryError("Can't run query due to a race , another started query", true)
  }

  private fun clearQueryPromise(): Option<Promise<QueryResult>> {
    this.queryPromiseReference.getAndSet(None)
  }

  private fun failQueryPromise(t: Throwable) {
    this.clearQueryPromise.foreach { promise ->
      log.error("Setting error on future {}", promise)
      promise.failure(t)
    }
  }

  private fun succeedQueryPromise(result: QueryResult) {
    this.queryResult = None
    this.currentQuery = None
    this.clearQueryPromise.foreach {
      _.success(result)
    }
  }

  private fun write(message: ClientMessage) {
    this.connectionHandler.write(message)
  }

  override fun toString: String {
    s"${this.getClass.getSimpleName}{counter=${this.currentCount}}"
  }
}