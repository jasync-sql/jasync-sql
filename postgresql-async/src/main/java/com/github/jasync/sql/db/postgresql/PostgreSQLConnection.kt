package com.github.jasync.sql.db.postgresql

import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.column.ColumnDecoderRegistry
import com.github.jasync.sql.db.column.ColumnEncoderRegistry
import com.github.jasync.sql.db.exceptions.ConnectionStillRunningQueryException
import com.github.jasync.sql.db.exceptions.InsufficientParametersException
import com.github.jasync.sql.db.general.MutableResultSet
import com.github.jasync.sql.db.pool.TimeoutScheduler
import com.github.jasync.sql.db.pool.TimeoutSchedulerPartialImpl
import com.github.jasync.sql.db.postgresql.codec.PostgreSQLConnectionDelegate
import com.github.jasync.sql.db.postgresql.codec.PostgreSQLConnectionHandler
import com.github.jasync.sql.db.postgresql.column.PostgreSQLColumnDecoderRegistry
import com.github.jasync.sql.db.postgresql.column.PostgreSQLColumnEncoderRegistry
import com.github.jasync.sql.db.postgresql.exceptions.GenericDatabaseException
import com.github.jasync.sql.db.postgresql.exceptions.MissingCredentialInformationException
import com.github.jasync.sql.db.postgresql.exceptions.QueryMustNotBeNullOrEmptyException
import com.github.jasync.sql.db.postgresql.messages.backend.AuthenticationChallengeCleartextMessage
import com.github.jasync.sql.db.postgresql.messages.backend.AuthenticationChallengeMD5
import com.github.jasync.sql.db.postgresql.messages.backend.AuthenticationChallengeMessage
import com.github.jasync.sql.db.postgresql.messages.backend.AuthenticationMessage
import com.github.jasync.sql.db.postgresql.messages.backend.AuthenticationOkMessage
import com.github.jasync.sql.db.postgresql.messages.backend.CommandCompleteMessage
import com.github.jasync.sql.db.postgresql.messages.backend.DataRowMessage
import com.github.jasync.sql.db.postgresql.messages.backend.ErrorMessage
import com.github.jasync.sql.db.postgresql.messages.backend.NotificationResponse
import com.github.jasync.sql.db.postgresql.messages.backend.ParameterStatusMessage
import com.github.jasync.sql.db.postgresql.messages.backend.PostgreSQLColumnData
import com.github.jasync.sql.db.postgresql.messages.backend.RowDescriptionMessage
import com.github.jasync.sql.db.postgresql.messages.frontend.ClientMessage
import com.github.jasync.sql.db.postgresql.messages.frontend.CredentialMessage
import com.github.jasync.sql.db.postgresql.messages.frontend.PreparedStatementExecuteMessage
import com.github.jasync.sql.db.postgresql.messages.frontend.PreparedStatementOpeningMessage
import com.github.jasync.sql.db.postgresql.messages.frontend.QueryMessage
import com.github.jasync.sql.db.postgresql.util.URLParser.DEFAULT
import com.github.jasync.sql.db.util.ExecutorServiceUtils
import com.github.jasync.sql.db.util.NettyUtils
import com.github.jasync.sql.db.util.Version
import com.github.jasync.sql.db.util.failure
import com.github.jasync.sql.db.util.isCompleted
import com.github.jasync.sql.db.util.length
import com.github.jasync.sql.db.util.map
import com.github.jasync.sql.db.util.onFailure
import com.github.jasync.sql.db.util.parseVersion
import com.github.jasync.sql.db.util.success
import com.github.jasync.sql.db.util.tryFailure
import io.netty.channel.EventLoopGroup
import mu.KotlinLogging
import java.util.Collections
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

private val logger = KotlinLogging.logger {}

class PostgreSQLConnection @JvmOverloads constructor(
        val configuration: Configuration = DEFAULT,
        val encoderRegistry: ColumnEncoderRegistry = PostgreSQLColumnEncoderRegistry.Instance,
        val decoderRegistry: ColumnDecoderRegistry = PostgreSQLColumnDecoderRegistry.Instance,
        val group: EventLoopGroup = NettyUtils.DefaultEventLoopGroup,
        val executionContext: ExecutorService = ExecutorServiceUtils.CommonPool
) : PostgreSQLConnectionDelegate, Connection, TimeoutScheduler by TimeoutSchedulerPartialImpl(executionContext) {

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

  private val parameterStatus = mutableMapOf<String, String>()
  private val parsedStatements = mutableMapOf<String, PreparedStatementHolder>()
  private var authenticated = false

  private val connectionFuture = CompletableFuture<Connection>()

  private var recentError = false
  private val queryPromiseReference = AtomicReference<Optional<CompletableFuture<QueryResult>>>(Optional.empty())
  private var currentQuery: Optional<MutableResultSet<PostgreSQLColumnData>> = Optional.empty()
  private var currentPreparedStatement: Optional<PreparedStatementHolder> = Optional.empty()
  private var version = Version(0, 0, 0)
  private val notifyListeners = Collections.synchronizedList(mutableListOf < (NotificationResponse) -> Unit>())

  private var queryResult: Optional<QueryResult> = Optional.empty()

  override fun eventLoopGroup(): EventLoopGroup = group
  fun isReadyForQuery(): Boolean = !this.queryPromise().isPresent

  override fun connect(): CompletableFuture<Connection> {
    this.connectionHandler.connect().onFailure(executionContext) { e ->
      this.connectionFuture.tryFailure(e)
    }

    return this.connectionFuture
  }

  override fun disconnect(): CompletableFuture<Connection> =
      this.connectionHandler.disconnect().toCompletableFuture().map(executionContext) { c -> this }
  override fun onTimeout() { disconnect() }

  override fun isConnected(): Boolean = this.connectionHandler.isConnected()

  fun parameterStatuses(): Map<String, String> = this.parameterStatus.toMap()

  override fun sendQuery(query: String): CompletableFuture<QueryResult> {
    validateQuery(query)

    val promise = CompletableFuture<QueryResult>()
    this.setQueryPromise(promise)

    write(QueryMessage(query))
    addTimeout(promise, configuration.queryTimeout)
    return promise
  }

  override fun sendPreparedStatement(query: String, values: List<Any?>): CompletableFuture<QueryResult> {
    validateQuery(query)

    val promise = CompletableFuture<QueryResult>()
    this.setQueryPromise(promise)

    val holder = this.parsedStatements.getOrPut(query
    ) {PreparedStatementHolder(query, preparedStatementsCounter.incrementAndGet())}

    if (holder.paramsCount != values.length) {
      this.clearQueryPromise()
      throw InsufficientParametersException(holder.paramsCount, values)
    }

    this.currentPreparedStatement = Optional.of(holder)
    this.currentQuery = Optional.of(MutableResultSet(holder.columnDatas))
    write(
        if (holder.prepared)
          PreparedStatementExecuteMessage(holder.statementId, holder.realQuery, values, this.encoderRegistry)
        else {
          holder.prepared = true
          PreparedStatementOpeningMessage(holder.statementId, holder.realQuery, values, this.encoderRegistry)
        })
    addTimeout(promise, configuration.queryTimeout)
    return promise
  }

  override fun onError(throwable: Throwable) {
    this.setErrorOnFutures(throwable)
  }

  fun hasRecentError(): Boolean = this.recentError

  private fun setErrorOnFutures(e: Throwable) {
    this.recentError = true

    logger.error("Error on connection", e)

    if (!this.connectionFuture.isCompleted) {
      this.connectionFuture.failure(e)
      this.disconnect()
    }

    this.currentPreparedStatement.map{p -> this.parsedStatements.remove(p.query)}
    this.currentPreparedStatement = Optional.empty()
    this.failQueryPromise(e)
  }

  override fun onReadyForQuery() {
    this.connectionFuture.success(this)

    this.recentError = false
    queryResult.ifPresent { this.succeedQueryPromise(it) }
  }

  override fun onError(message: ErrorMessage) {
    logger.error("Error , message -> {}", message)

    val error = GenericDatabaseException(message)
    error.fillInStackTrace()

    this.setErrorOnFutures(error)
  }

  override fun onCommandComplete(message: CommandCompleteMessage) {
    this.currentPreparedStatement = Optional.empty()
    queryResult = Optional.of(QueryResult(message.rowsAffected.toLong(), message.statusMessage, this.currentQuery.get()))
  }

  override fun onParameterStatus(message: ParameterStatusMessage) {
    this.parameterStatus.put(message.key, message.value)
    if (ServerVersionKey == message.key) {
      this.version = parseVersion(message.value)
    }
  }

  override fun onDataRow(message: DataRowMessage) {
    val items = Array(message.values.size) {
      val buf = message.values[it]
      if (buf == null) {
        null
      } else {
        try {
          val columnType = this.currentQuery.get().columnTypes[it]
          this.decoderRegistry.decode(columnType, buf, configuration.charset)
        } finally {
          buf.release()
        }
      }
    }

    this.currentQuery.get().addRow(items)
  }

  override fun onRowDescription(message: RowDescriptionMessage) {
    this.currentQuery = Optional.of(MutableResultSet(message.columnDatas))
    this.setColumnDatas(message.columnDatas)
  }

  private fun setColumnDatas(columnDatas: List<PostgreSQLColumnData>) {
    this.currentPreparedStatement.ifPresent { holder ->
      holder.columnDatas = columnDatas
    }
  }

  override fun onAuthenticationResponse(message: AuthenticationMessage) {

    when (message) {
      is AuthenticationOkMessage
      -> {
        logger.debug("Successfully logged in to database")
        this.authenticated = true
      }
      is AuthenticationChallengeCleartextMessage
      -> {
        write(this.credential(message))
      }
      is AuthenticationChallengeMD5
      -> {
        write(this.credential(message))
      }
    }

  }

  override fun onNotificationResponse(message: NotificationResponse) {
    val iterator = this.notifyListeners.iterator()
    while (iterator.hasNext()) {
      iterator.next().invoke(message)
    }
  }

  fun registerNotifyListener(listener: (NotificationResponse) -> Unit )
  {
    this.notifyListeners.add(listener)
  }

  fun unregisterNotifyListener(listener: (NotificationResponse) -> Unit )
  {
    this.notifyListeners.remove(listener)
  }

  fun clearNotifyListeners() {
    this.notifyListeners.clear()
  }

  private fun credential(authenticationMessage: AuthenticationChallengeMessage): CredentialMessage {
    return if (configuration.username != null && configuration.password != null) {
      CredentialMessage(
          configuration.username,
          configuration.password!!,
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

  private
  fun notReadyForQueryError(errorMessage: String, race: Boolean) {
    logger.error(errorMessage)
    throw ConnectionStillRunningQueryException(
        this.currentCount,
        race
    )
  }

  fun validateIfItIsReadyForQuery(errorMessage: String) {
    if (this.queryPromise().isPresent)
      notReadyForQueryError(errorMessage, false)
  }

  private fun validateQuery(query: String) {
    if (!this.isConnected()) {
      throw IllegalStateException("not connected so can't execute queries. please make sure connect() was called and disconnect() was not called.")
    }
    this.validateIfItIsReadyForQuery("Can't run query because there is one query pending already")

    if (query == null || query.isEmpty()) {
      throw QueryMustNotBeNullOrEmptyException(query)
    }
  }

  private fun queryPromise(): Optional<CompletableFuture<QueryResult>> = queryPromiseReference.get()

  private fun setQueryPromise(promise: CompletableFuture<QueryResult>) {
    if (!this.queryPromiseReference.compareAndSet(Optional.empty(), Optional.of(promise)))
      notReadyForQueryError("Can't run query due to a race , another started query", true)
  }

  private fun clearQueryPromise(): Optional<CompletableFuture<QueryResult>> {
    return this.queryPromiseReference.getAndSet(Optional.empty())
  }

  private fun failQueryPromise(t: Throwable) {
    this.clearQueryPromise().ifPresent { promise ->
      logger.error("Setting error on future {}", promise)
      promise.failure(t)
    }
  }

  private fun succeedQueryPromise(result: QueryResult) {
    this.queryResult = Optional.empty()
    this.currentQuery = Optional.empty()
    this.clearQueryPromise().ifPresent {
      it.success(result)
    }
  }

  private fun write(message: ClientMessage) {
    this.connectionHandler.write(message)
  }

  override fun toString(): String {
    return "${this.javaClass.simpleName}{counter=${this.currentCount}}"
  }
}
