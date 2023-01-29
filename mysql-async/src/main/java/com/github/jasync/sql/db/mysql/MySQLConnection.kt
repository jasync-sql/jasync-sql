package com.github.jasync.sql.db.mysql

import com.github.jasync.sql.db.ConcreteConnectionBase
import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.EMPTY_RESULT_SET
import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.ResultSet
import com.github.jasync.sql.db.SSLConfiguration
import com.github.jasync.sql.db.exceptions.ConnectionStillRunningQueryException
import com.github.jasync.sql.db.exceptions.DatabaseException
import com.github.jasync.sql.db.exceptions.InsufficientParametersException
import com.github.jasync.sql.db.interceptor.PreparedStatementParams
import com.github.jasync.sql.db.mysql.codec.MySQLConnectionHandler
import com.github.jasync.sql.db.mysql.codec.MySQLHandlerDelegate
import com.github.jasync.sql.db.mysql.exceptions.MySQLException
import com.github.jasync.sql.db.mysql.message.client.AuthenticationSwitchResponse
import com.github.jasync.sql.db.mysql.message.client.CapabilityRequestMessage
import com.github.jasync.sql.db.mysql.message.client.HandshakeResponseMessage
import com.github.jasync.sql.db.mysql.message.server.AuthenticationSwitchRequest
import com.github.jasync.sql.db.mysql.message.server.EOFMessage
import com.github.jasync.sql.db.mysql.message.server.ErrorMessage
import com.github.jasync.sql.db.mysql.message.server.HandshakeMessage
import com.github.jasync.sql.db.mysql.message.server.OkMessage
import com.github.jasync.sql.db.mysql.util.CapabilityFlag
import com.github.jasync.sql.db.mysql.util.CharsetMapper
import com.github.jasync.sql.db.pool.TimeoutScheduler
import com.github.jasync.sql.db.pool.TimeoutSchedulerImpl
import com.github.jasync.sql.db.util.Failure
import com.github.jasync.sql.db.util.NettyUtils
import com.github.jasync.sql.db.util.Success
import com.github.jasync.sql.db.util.Version
import com.github.jasync.sql.db.util.complete
import com.github.jasync.sql.db.util.failed
import com.github.jasync.sql.db.util.flatMap
import com.github.jasync.sql.db.util.isCompleted
import com.github.jasync.sql.db.util.length
import com.github.jasync.sql.db.util.mapTry
import com.github.jasync.sql.db.util.onCompleteAsync
import com.github.jasync.sql.db.util.onFailureAsync
import com.github.jasync.sql.db.util.parseVersion
import com.github.jasync.sql.db.util.success
import com.github.jasync.sql.db.util.toCompletableFuture
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.ssl.SslHandler
import mu.KotlinLogging
import java.time.Duration
import java.util.Optional
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

private val logger = KotlinLogging.logger {}

@Suppress("CanBeParameter")
class MySQLConnection @JvmOverloads constructor(
    configuration: Configuration,
    charsetMapper: CharsetMapper = CharsetMapper.Instance,
    withDelegate: (delegate: MySQLHandlerDelegate) -> MySQLHandlerDelegate = { delegate -> delegate }
) : ConcreteConnectionBase(configuration), MySQLHandlerDelegate, Connection, TimeoutScheduler {

    companion object {
        val Counter = AtomicLong()

        @Suppress("unused")
        val MicrosecondsVersion = Version(5, 6, 0)
        private val regexForCallInQueryStart = Regex("\\s*call\\s+.*", RegexOption.IGNORE_CASE)
        const val CLIENT_FOUND_ROWS_PROP_NAME = "jasync.mysql.CLIENT_FOUND_ROWS"
    }

    init {
        // validate that this charset is supported
        charsetMapper.toInt(configuration.charset)
    }

    private val connectionCount = Counter.incrementAndGet()
    private val connectionId = "<mysql-connection-$connectionCount>"
    override val id: String = connectionId

    private val connectionHandler = MySQLConnectionHandler(
        configuration,
        charsetMapper,
        withDelegate(this),
        configuration.eventLoopGroup,
        configuration.executionContext,
        connectionId
    )

    private val connectionPromise = CompletableFuture<MySQLConnection>()
    private val disconnectionPromise = CompletableFuture<Connection>()

    private val queryPromiseReference = AtomicReference<Optional<CompletableFuture<QueryResult>>>(Optional.empty())
    private var isStoredProcedureCall = false
    private var lastResultSet: ResultSet = EMPTY_RESULT_SET
    private var connected = false
    private var lastException: Throwable? = null
    private var serverVersion: Version? = null

    object StatusFlags {
        // https://dev.mysql.com/doc/internals/en/status-flags.html
        // private val IN_TRANSACTION: Int = 1
        internal const val AUTO_COMMIT: Int = 2
    }

    private var serverStatus: Int = 0

    fun isAutoCommit(): Boolean = (serverStatus and StatusFlags.AUTO_COMMIT) != 0

    private val queryTimeoutSchedulerImpl =
        TimeoutSchedulerImpl(configuration.executionContext, configuration.eventLoopGroup, this::onQueryTimeout)
    private val createTimeoutSchedulerImpl =
        TimeoutSchedulerImpl(configuration.executionContext, configuration.eventLoopGroup, this::onCreateTimeout)

    private var channelClosed = false
    private var reportErrorAfterChannelClosed = false

    @Suppress("unused")
    fun version() = this.serverVersion

    override fun lastException(): Throwable? = this.lastException

    override fun connect(): CompletableFuture<MySQLConnection> {
        createTimeoutSchedulerImpl.addTimeout(
            this.connectionPromise,
            Duration.ofMillis(configuration.connectionTimeout.toLong()),
            connectionId
        )
        this.connectionHandler.connect().onFailureAsync(configuration.executionContext) { e ->
            this.connectionPromise.failed(e)
        }

        return this.connectionPromise
    }

    fun close(): CompletableFuture<Connection> {
        logger.trace { "close connection $connectionId" }
        channelClosed = true
        val exception = DatabaseException("Connection is being closed")
        this.failQueryPromise(exception)
        if (this.isConnected()) {
            if (!this.disconnectionPromise.isCompleted) {
                logger.trace { "send quit message $connectionId" }
                this.connectionHandler.sendQuitMessage()
                    .onCompleteAsync(configuration.executionContext) { ty1 ->
                        when (ty1) {
                            is Success -> {
                                logger.trace { "close channel $connectionId" }
                                this.connectionHandler.closeChannel().toCompletableFuture()
                                    .onCompleteAsync(configuration.executionContext) { ty2 ->
                                        logger.trace { "channel was closed $connectionId" }
                                        when (ty2) {
                                            is Success -> this.disconnectionPromise.complete(this)
                                            is Failure -> this.disconnectionPromise.complete(ty2)
                                        }
                                    }
                            }

                            is Failure -> {
                                this.connectionHandler.closeChannel()
                                this.disconnectionPromise.complete(ty1)
                            }
                        }
                    }
            }
        }

        return this.disconnectionPromise
    }

    override fun unregistered() {
        logger.debug {
            if (isQuerying()) {
                "$id - client got disconnected in the middle of query execution"
            } else {
                "$id - client got disconnected with no running query"
            }
        }
        close().mapTry { _, throwable ->
            if (throwable != null) {
                logger.warn(throwable) { "failed to unregister $connectionId" }
            }
        }
    }

    override fun isTimeout(): Boolean = queryTimeoutSchedulerImpl.isTimeout()

    override fun connected(ctx: ChannelHandlerContext) {
        logger.debug { "$connectionId Connected to ${ctx.channel().remoteAddress()}" }
        this.connected = true
    }

    override fun exceptionCaught(exception: Throwable) {
        if (channelClosed) {
            logger.trace(exception) { "$connectionId Transport failure after connection close" }
            if (!reportErrorAfterChannelClosed) {
                logger.info { "$connectionId Transport failure after connection close: ${exception.message}" }
                reportErrorAfterChannelClosed = true
            }
        }
        logger.error("$connectionId Transport failure ", exception)
        setException(exception)
    }

    override fun onError(message: ErrorMessage) {
        logger.error("$connectionId Received an error message -> {}", message)
        val exception = MySQLException(message)
        this.setException(exception)
    }

    private fun setException(t: Throwable) {
        this.lastException = t
        this.connectionPromise.failed(t)
        this.failQueryPromise(t)
    }

    override fun onOk(message: OkMessage) {
        this.serverStatus = message.statusFlags
        if (!this.connectionPromise.isCompleted) {
            logger.debug("$connectionId Connected to database")
            this.connectionPromise.success(this)
        } else {
            if (this.isQuerying()) {
                if (isStoredProcedureCall) {
                    this.succeedQueryPromise(
                        MySQLQueryResult(
                            message.affectedRows,
                            message.message,
                            message.lastInsertId,
                            message.statusFlags,
                            message.warnings,
                            lastResultSet
                        )
                    )
                } else {
                    this.succeedQueryPromise(
                        MySQLQueryResult(
                            message.affectedRows,
                            message.message,
                            message.lastInsertId,
                            message.statusFlags,
                            message.warnings
                        )
                    )
                }
            } else {
                logger.warn("$connectionId Received OK when not querying or connecting, not sure what this is: $message")
            }
        }
    }

    override fun onEOF(message: EOFMessage) {
        logger.debug { "$connectionId onEOF isStoredProcedureCall=$isStoredProcedureCall isQuerying=${isQuerying()}" }
        this.serverStatus = message.flags
        if (this.isQuerying() && !isStoredProcedureCall) {
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
        this.serverStatus = message.statusFlags

        val switchToSsl = when (this.configuration.ssl.mode) {
            SSLConfiguration.Mode.Disable -> false
            SSLConfiguration.Mode.Prefer -> message.supportsSSL()
            SSLConfiguration.Mode.Require,
            SSLConfiguration.Mode.VerifyCA,
            SSLConfiguration.Mode.VerifyFull -> {
                require(message.supportsSSL()) { "SSL is not supported on server" }
                true
            }
        }

        val clientFoundRows = System.getProperty(CLIENT_FOUND_ROWS_PROP_NAME) != null
        if (clientFoundRows) {
            logger.debug { "CLIENT_FOUND_ROWS capability set" }
        }

        val capabilities = CapabilityRequestMessage(
            setOfNotNull(
                CapabilityFlag.CLIENT_PLUGIN_AUTH,
                CapabilityFlag.CLIENT_FOUND_ROWS.takeIf { clientFoundRows },
                CapabilityFlag.CLIENT_PROTOCOL_41,
                CapabilityFlag.CLIENT_TRANSACTIONS,
                CapabilityFlag.CLIENT_MULTI_RESULTS,
                CapabilityFlag.CLIENT_SECURE_CONNECTION,
                CapabilityFlag.CLIENT_SSL.takeIf { switchToSsl },
                CapabilityFlag.CLIENT_CONNECT_WITH_DB.takeIf { configuration.database != null },
                CapabilityFlag.CLIENT_CONNECT_ATTRS.takeIf { configuration.applicationName != null }
            )
        )

        val handshakeResponse = HandshakeResponseMessage(
            capabilities,
            configuration.username,
            configuration.charset,
            message.seed,
            message.authenticationMethod,
            database = configuration.database,
            password = configuration.password,
            appName = configuration.applicationName
        )

        if (!switchToSsl) {
            connectionHandler.write(handshakeResponse)
            return
        }

        val channelFuture = connectionHandler.write(capabilities)
        channelFuture.addListener { sslRequestFuture ->
            // connectionHandler.write will handle errors (logging, failing promise, etc) in this case.
            if (!sslRequestFuture.isSuccess) return@addListener
            val channel = channelFuture.channel()
            val handler = try {
                val sslContext = NettyUtils.createSslContext(configuration.ssl)
                val sslEngine = sslContext.newEngine(channel.alloc(), configuration.host, configuration.port)
                if (configuration.ssl.mode == SSLConfiguration.Mode.VerifyFull) {
                    NettyUtils.verifyHostIdentity(sslEngine)
                }
                SslHandler(sslEngine)
            } catch (e: Exception) {
                logger.error(e) { "Creating SSL Engine failed" }
                setException(e)
                return@addListener
            }
            channel.pipeline().addFirst(handler)
            handler.handshakeFuture().addListener { handshakeFuture ->
                if (handshakeFuture.isSuccess) {
                    connectionHandler.write(handshakeResponse)
                } else {
                    logger.error(handshakeFuture.cause()) { "SSL Handshake failed" }
                    handshakeFuture.cause()?.let(::setException)
                }
            }
        }
    }

    override fun switchAuthentication(message: AuthenticationSwitchRequest) {
        this.connectionHandler.write(AuthenticationSwitchResponse(configuration.password, message))
    }

    fun sendQueryAfterCurrent(query: String): CompletableFuture<QueryResult> {
        return if (isQuerying()) {
            logger.info { "attaching after current query $query" }
            queryPromise().get().flatMap { sendQuery(query) }
        } else {
            sendQuery(query)
        }.mapTry { queryResult, throwable ->
            // Cancellation exception will be ignored so that the following transaction clean up can be executed
            if (throwable is CancellationException) QueryResult(rowsAffected = 0L, statusMessage = null)
            else queryResult
        }
    }

    override fun sendQueryDirect(query: String): CompletableFuture<QueryResult> {
        logger.trace { "$connectionId sendQuery() - $query" }
        this.validateIsReadyForQuery()
        val promise = CompletableFuture<QueryResult>()
        this.setQueryPromise(promise)
        this.checkStoredProcedureCall(query)
        this.connectionHandler.sendQuery(query)
        queryTimeoutSchedulerImpl.addTimeout(promise, configuration.queryTimeout, connectionId)
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

    override fun isQuerying(): Boolean = this.queryPromise().isPresent

    override fun onResultSet(resultSet: ResultSet, message: EOFMessage) {
        if (this.isQuerying()) {
            if (isStoredProcedureCall) {
                lastResultSet = resultSet
            } else {
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
        } else {
            logger.warn { "$connectionId onResultSet - called without active query" }
        }
    }

    override fun disconnect(): CompletableFuture<Connection> = this.close()

    private fun onQueryTimeout() {
        disconnect()
    }

    private fun onCreateTimeout() {
        disconnect()
    }

    override fun isConnected(): Boolean = this.connectionHandler.isConnected()

    override fun hasRecentError(): Boolean = lastException != null

    @Suppress("UnnecessaryVariable")
    override fun sendPreparedStatementDirect(params: PreparedStatementParams): CompletableFuture<QueryResult> {
        logger.trace { "$connectionId sendPreparedStatement() - $params" }
        this.validateIsReadyForQuery()
        val totalParameters = params.query.count { it == '?' }
        if (params.values.length != totalParameters) {
            throw InsufficientParametersException(totalParameters, params.values)
        }
        val promise = CompletableFuture<QueryResult>()
        this.setQueryPromise(promise)
        this.checkStoredProcedureCall(params.query)
        this.connectionHandler.sendPreparedStatement(params.query, params.values)
        queryTimeoutSchedulerImpl.addTimeout(promise, configuration.queryTimeout, connectionId)
        val closedPromise = this.releaseIfNeeded(params.release, promise, params.query)
        return closedPromise
    }

    private fun checkStoredProcedureCall(query: String) {
        if (query.matches(regexForCallInQueryStart)) {
            isStoredProcedureCall = true
        }
    }

    override fun releasePreparedStatement(query: String): CompletableFuture<Boolean> {
        this.validateIsReadyForQuery()
        return this.connectionHandler.closePreparedStatement(query)
    }

    override fun toString(): String {
        return "%s(%s,%d)".format(this::class.java.name, this.connectionId, this.connectionCount)
    }

    private fun validateIsReadyForQuery() {
        if (!this.isConnected()) {
            throw IllegalStateException("not connected so can't execute queries. please make sure connect() was called and disconnect() was not called.")
        }
        if (isQuerying()) {
            throw ConnectionStillRunningQueryException(this.id, false)
        }
    }

    private fun queryPromise(): Optional<CompletableFuture<QueryResult>> = queryPromiseReference.get()

    private fun setQueryPromise(promise: CompletableFuture<QueryResult>) {
        if (!this.queryPromiseReference.compareAndSet(Optional.empty(), Optional.of(promise)))
            throw ConnectionStillRunningQueryException(this.id, true)
    }

    private fun clearQueryPromise(): Optional<CompletableFuture<QueryResult>> {
        val currentPromise = this.queryPromiseReference.getAndSet(Optional.empty())
        if (currentPromise.isPresent) {
            isStoredProcedureCall = false
            lastResultSet = EMPTY_RESULT_SET
        }
        return currentPromise
    }
}
