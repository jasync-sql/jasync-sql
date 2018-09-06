package com.github.jasync.sql.db.mysql

import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.ResultSet
import com.github.jasync.sql.db.exceptions.ConnectionStillRunningQueryException
import com.github.jasync.sql.db.exceptions.DatabaseException
import com.github.jasync.sql.db.exceptions.InsufficientParametersException
import com.github.jasync.sql.db.mysql.codec.MySQLConnectionHandler
import com.github.jasync.sql.db.mysql.codec.MySQLHandlerDelegate
import com.github.jasync.sql.db.mysql.exceptions.MySQLException
import com.github.jasync.sql.db.mysql.message.client.AuthenticationSwitchResponse
import com.github.jasync.sql.db.mysql.message.client.HandshakeResponseMessage
import com.github.jasync.sql.db.mysql.message.client.QueryMessage
import com.github.jasync.sql.db.mysql.message.client.QuitMessage
import com.github.jasync.sql.db.mysql.message.server.AuthenticationSwitchRequest
import com.github.jasync.sql.db.mysql.message.server.EOFMessage
import com.github.jasync.sql.db.mysql.message.server.ErrorMessage
import com.github.jasync.sql.db.mysql.message.server.HandshakeMessage
import com.github.jasync.sql.db.mysql.message.server.OkMessage
import com.github.jasync.sql.db.mysql.util.CharsetMapper
import com.github.jasync.sql.db.pool.TimeoutScheduler
import com.github.jasync.sql.db.pool.TimeoutSchedulerPartialImpl
import com.github.jasync.sql.db.util.ExecutorServiceUtils
import com.github.jasync.sql.db.util.NettyUtils
import com.github.jasync.sql.db.util.Try
import com.github.jasync.sql.db.util.Version
import com.github.jasync.sql.db.util.complete
import com.github.jasync.sql.db.util.failed
import com.github.jasync.sql.db.util.isCompleted
import com.github.jasync.sql.db.util.length
import com.github.jasync.sql.db.util.onComplete
import com.github.jasync.sql.db.util.onFailure
import com.github.jasync.sql.db.util.parseVersion
import com.github.jasync.sql.db.util.success
import com.github.jasync.sql.db.util.toCompletableFuture
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.EventLoopGroup
import mu.KotlinLogging
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference


class MySQLConnection @JvmOverloads constructor(
    val configuration: Configuration,
    charsetMapper: CharsetMapper = CharsetMapper.Instance,
    private val group: EventLoopGroup = NettyUtils.DefaultEventLoopGroup,
    private val executionContext: Executor = ExecutorServiceUtils.CommonPool
) : MySQLHandlerDelegate
    , Connection
    , TimeoutScheduler by TimeoutSchedulerPartialImpl(executionContext) {

  companion object {
    val Counter = AtomicLong()
    val MicrosecondsVersion = Version(5, 6, 0)
  }

  init {
    // validate that this charset is supported
    charsetMapper.toInt(configuration.charset)
  }

  private val connectionCount = MySQLConnection.Counter.incrementAndGet()
  private val connectionId = "<mysql-connection-$connectionCount>"

  private val connectionHandler = MySQLConnectionHandler(
      configuration,
      charsetMapper,
      this,
      group,
      executionContext,
      connectionId)

  private val connectionPromise = CompletableFuture<Connection>()
  private val disconnectionPromise = CompletableFuture<Connection>()

  private val queryPromiseReference = AtomicReference<Optional<CompletableFuture<QueryResult>>>(Optional.empty())
  private var connected = false
  private var _lastException: Throwable? = null
  private var serverVersion: Version? = null

  fun version() = this.serverVersion
  fun lastException(): Throwable? = this?._lastException
  fun count(): Long = this.connectionCount

  override fun eventLoopGroup(): EventLoopGroup = group

  override fun connect(): CompletableFuture<Connection> {
    this.connectionHandler.connect().onFailure(executionContext) { e ->
      this.connectionPromise.failed(e)
    }

    return this.connectionPromise
  }
  override fun isTimeout(): Boolean {
    //TODO check this
    return false
  }

  fun close(): CompletableFuture<Connection> {
    if (this.isConnected()) {
      if (!this.disconnectionPromise.isCompleted) {
        val exception = DatabaseException("Connection is being closed")
        exception.fillInStackTrace()
        this.failQueryPromise(exception)
        this.connectionHandler.clearQueryState()
        this.connectionHandler.write(QuitMessage.Instance).toCompletableFuture().onComplete(executionContext) { ty1 ->
          when (ty1) {
            is Try.Success -> {
              this.connectionHandler.disconnect().toCompletableFuture().onComplete(executionContext) { ty2 ->
                when (ty2) {
                  is Try.Success -> this.disconnectionPromise.complete(this)
                  is Try.Failure -> this.disconnectionPromise.complete(ty2)
                }
              }
            }
            is Try.Failure -> this.disconnectionPromise.complete(ty1)
          }
        }
      }
    }

    return this.disconnectionPromise
  }

  override fun connected(ctx: ChannelHandlerContext) {
    logger.debug("Connected to {}", ctx.channel().remoteAddress())
    this.connected = true
  }

  override fun exceptionCaught(exception: Throwable) {
    logger.error("Transport failure ", exception)
    setException(exception)
  }

  override fun onError(message: ErrorMessage) {
    logger.error("Received an error message -> {}", message)
    val exception = MySQLException(message)
    exception.fillInStackTrace()
    this.setException(exception)
  }

  private fun setException(t: Throwable) {
    this._lastException = t
    this.connectionPromise.failed(t)
    this.failQueryPromise(t)
  }

  override fun onOk(message: OkMessage) {
    if (!this.connectionPromise.isCompleted) {
      logger.debug("Connected to database")
      this.connectionPromise.success(this)
    } else {
      if (this.isQuerying()) {
        this.succeedQueryPromise(
            MySQLQueryResult(
                message.affectedRows,
                message.message,
                message.lastInsertId,
                message.statusFlags,
                message.warnings
            )
        )
      } else {
        logger.warn("Received OK when not querying or connecting, not sure what this is")
      }
    }
  }

  override fun onEOF(message: EOFMessage) {
    if (this.isQuerying()) {
      this.succeedQueryPromise(
          MySQLQueryResult(
              0,
              null,
              -1,
              message.flags,
              message.warningCount
          )
      )
    }
  }

  override fun onHandshake(message: HandshakeMessage) {
    this.serverVersion = parseVersion(message.serverVersion)

    this.connectionHandler.write(HandshakeResponseMessage(
        configuration.username,
        configuration.charset,
        message.seed,
        message.authenticationMethod,
        database = configuration.database,
        password = configuration.password
    ))
  }

  override fun switchAuthentication(message: AuthenticationSwitchRequest) {
    this.connectionHandler.write(AuthenticationSwitchResponse(configuration.password, message))
  }

  override fun sendQuery(query: String): CompletableFuture<QueryResult> {
    this.validateIsReadyForQuery()
    val promise = CompletableFuture<QueryResult>()
    this.setQueryPromise(promise)
    this.connectionHandler.write(QueryMessage(query))
    addTimeout(promise, configuration.queryTimeout)
    return promise
  }

  private fun failQueryPromise(t: Throwable) {
    this.clearQueryPromise().ifPresent {
      it.failed(t)
    }
  }

  private fun succeedQueryPromise(queryResult: QueryResult) {

    this.clearQueryPromise().ifPresent {
      it.success(queryResult)
    }

  }

  fun isQuerying(): Boolean = this.queryPromise().isPresent

  override fun onResultSet(resultSet: ResultSet, message: EOFMessage) {
    if (this.isQuerying()) {
      this.succeedQueryPromise(
          MySQLQueryResult(
              resultSet.size.toLong(),
              null,
              -1,
              message.flags,
              message.warningCount,
              resultSet
          )
      )
    }
  }

  override fun disconnect(): CompletableFuture<Connection> = this.close()
  override fun onTimeout(): Unit {disconnect()}

  override fun isConnected(): Boolean = this.connectionHandler.isConnected()

  override fun sendPreparedStatement(query: String, values: List<Any?>): CompletableFuture<QueryResult> {
    this.validateIsReadyForQuery()
    val totalParameters = query.count { it == '?' }
    if (values.length != totalParameters) {
      throw InsufficientParametersException(totalParameters, values)
    }
    val promise = CompletableFuture<QueryResult>()
    this.setQueryPromise(promise)
    this.connectionHandler.sendPreparedStatement(query, values)
    addTimeout(promise, configuration.queryTimeout)
    return promise
  }


  override fun toString(): String {
    return "%s(%s,%d)".format(this::class.java.name, this.connectionId, this.connectionCount)
  }

  private fun validateIsReadyForQuery() {
    if (isQuerying()) {
      throw ConnectionStillRunningQueryException(this.connectionCount, false)
    }
  }

  private fun queryPromise(): Optional<CompletableFuture<QueryResult>> = queryPromiseReference.get()

  private fun setQueryPromise(promise: CompletableFuture<QueryResult>) {
    if (!this.queryPromiseReference.compareAndSet(Optional.empty(), Optional.of(promise)))
      throw ConnectionStillRunningQueryException(this.connectionCount, true)
  }

  private fun clearQueryPromise(): Optional<CompletableFuture<QueryResult>> {
    return this.queryPromiseReference.getAndSet(Optional.empty())
  }

}

private val logger = KotlinLogging.logger {}
