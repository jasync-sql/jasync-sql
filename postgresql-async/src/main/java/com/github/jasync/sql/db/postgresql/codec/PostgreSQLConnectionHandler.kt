package com.github.jasync.sql.db.postgresql.codec

import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.SSLConfiguration
import com.github.jasync.sql.db.column.ColumnEncoderRegistry
import com.github.jasync.sql.db.postgresql.exceptions.QueryMustNotBeNullOrEmptyException
import com.github.jasync.sql.db.postgresql.messages.backend.*
import com.github.jasync.sql.db.postgresql.messages.frontend.ClientMessage
import com.github.jasync.sql.db.postgresql.messages.frontend.CloseMessage
import com.github.jasync.sql.db.postgresql.messages.frontend.SSLRequestMessage
import com.github.jasync.sql.db.postgresql.messages.frontend.StartupMessage
import com.github.jasync.sql.db.util.*
import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.handler.codec.CodecException
import io.netty.handler.ssl.SslHandler
import mu.KotlinLogging
import java.net.InetSocketAddress
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

private val logger = KotlinLogging.logger {}

@Suppress("MemberVisibilityCanBePrivate")
class PostgreSQLConnectionHandler(
    val configuration: Configuration,
    val encoderRegistry: ColumnEncoderRegistry,
    val connectionDelegate: PostgreSQLConnectionDelegate,
    val group: EventLoopGroup,
    val executionContext: Executor = ExecutorServiceUtils.CommonPool
) : SimpleChannelInboundHandler<Any>() {

    private val properties = listOf(
        "user" to configuration.username,
        "database" to configuration.database,
        "client_encoding" to configuration.charset.name(),
        "DateStyle" to "ISO",
        "extra_float_digits" to "2",
        "search_path" to configuration.currentSchema
    ).filter { it.second != null }

    //  private val executionContext: Executor = ExecutorServiceUtils.CommonPool
    private val bootstrap = Bootstrap()
    private val connectionFuture = CompletableFuture<PostgreSQLConnectionHandler>()
    private val disconnectionPromise = CompletableFuture<PostgreSQLConnectionHandler>()
    private var processData: ProcessData? = null
    private var currentContext: ChannelHandlerContext? = null

    fun connect(): CompletableFuture<PostgreSQLConnectionHandler> {
        this.bootstrap.group(this.group)
        this.bootstrap.channel(NettyUtils.getSocketChannelClass(this.group, configuration.unixSocket != null))
        this.bootstrap.handler(object : ChannelInitializer<Channel>() {
            override fun initChannel(ch: Channel) {
                ch.pipeline().addLast(
                    MessageDecoder(
                        configuration.ssl.mode != SSLConfiguration.Mode.Disable,
                        configuration.charset,
                        configuration.maximumMessageSize
                    ),
                    MessageEncoder(configuration.charset, encoderRegistry),
                    this@PostgreSQLConnectionHandler
                )
            }
        })
        this.bootstrap.option<Boolean>(ChannelOption.SO_KEEPALIVE, true)
        this.bootstrap.option(ChannelOption.ALLOCATOR, configuration.allocator)
        this.bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.configuration.connectionTimeout)
        this.bootstrap.connect(configuration.unixSocket ?: InetSocketAddress(configuration.host, configuration.port))
            .onFailure(executionContext) { e ->
                connectionFuture.failed(e)
            }
        return this.connectionFuture
    }

    fun disconnect(): CompletableFuture<PostgreSQLConnectionHandler> {
        if (isConnected()) {
            this.currentContext!!.channel().writeAndFlush(CloseMessage).toCompletableFuture()
                .onCompleteAsync(executionContext) { ty1 ->
                    when (ty1) {
                        is Success -> {
                            ty1.get().channel().close().toCompletableFuture().onCompleteAsync(executionContext) { ty2 ->
                                when (ty2) {
                                    is Success -> this.disconnectionPromise.success(this)
                                    is Failure -> this.disconnectionPromise.failed(ty2.exception)
                                }
                            }
                        }
                        is Failure -> {
                            this.currentContext!!.channel().close()
                            this.disconnectionPromise.failed(ty1.exception)
                        }
                    }
                }
        }

        return this.disconnectionPromise
    }

    fun isConnected(): Boolean {
        return this.currentContext?.channel()?.isActive ?: false
    }

    @Suppress("RedundantUnitReturnType")
    override fun channelActive(ctx: ChannelHandlerContext) {
        if (configuration.ssl.mode == SSLConfiguration.Mode.Disable)
            ctx.writeAndFlush(StartupMessage(this.properties))
        else
            ctx.writeAndFlush(SSLRequestMessage)
    }

    override fun channelRead0(ctx: ChannelHandlerContext?, message: Any) {
        logger.trace { "got message $message" }
        when (message) {
            SSLResponseMessage(true) -> {
                val sslContext = NettyUtils.createSslContext(configuration.ssl)
                val sslEngine = sslContext.newEngine(ctx!!.alloc(), configuration.host, configuration.port)
                if (configuration.ssl.mode == SSLConfiguration.Mode.VerifyFull) {
                    NettyUtils.verifyHostIdentity(sslEngine)
                }
                val handler = SslHandler(sslEngine)
                ctx.pipeline().addFirst(handler)
                handler.handshakeFuture().addListener { future ->
                    if (future.isSuccess) {
                        ctx.writeAndFlush(StartupMessage(properties))
                    } else {
                        connectionDelegate.onError(future.cause())
                    }
                }
            }

            SSLResponseMessage(false) -> if (configuration.ssl.mode < SSLConfiguration.Mode.Require) {
                ctx!!.writeAndFlush(StartupMessage(properties))
            } else {
                connectionDelegate.onError(IllegalArgumentException("SSL is not supported on server"))
            }

            is ServerMessage -> {
                when (message.kind) {
                    ServerMessage.BackendKeyData -> {
                        this.processData = message as ProcessData
                    }
                    ServerMessage.BindComplete -> {
                    }
                    ServerMessage.Authentication -> {
                        logger.debug("Authentication response received $message")
                        connectionDelegate.onAuthenticationResponse(message as AuthenticationMessage)
                    }
                    ServerMessage.CommandComplete -> {
                        connectionDelegate.onCommandComplete(message as CommandCompleteMessage)
                    }
                    ServerMessage.CloseComplete -> {
                        connectionDelegate.onCloseComplete()
                    }
                    ServerMessage.DataRow -> {
                        connectionDelegate.onDataRow(message as DataRowMessage)
                    }
                    ServerMessage.Error -> {
                        connectionDelegate.onError(message as ErrorMessage)
                    }
                    ServerMessage.EmptyQueryString -> {
                        val exception = QueryMustNotBeNullOrEmptyException("")
                        connectionDelegate.onError(exception)
                    }
                    ServerMessage.NoData -> {
                    }
                    ServerMessage.Notice -> {
                        logger.info("Received notice $message")
                    }
                    ServerMessage.NotificationResponse -> {
                        connectionDelegate.onNotificationResponse(message as NotificationResponse)
                    }
                    ServerMessage.ParameterStatus -> {
                        connectionDelegate.onParameterStatus(message as ParameterStatusMessage)
                    }
                    ServerMessage.ParseComplete -> {
                    }
                    ServerMessage.ReadyForQuery -> {
                        connectionDelegate.onReadyForQuery()
                    }
                    ServerMessage.RowDescription -> {
                        connectionDelegate.onRowDescription(message as RowDescriptionMessage)
                    }
                    else -> {
                        val exception =
                            IllegalStateException("Handler not implemented for message %s".format(message.kind))
                        connectionDelegate.onError(exception)
                    }
                }
            }
            else -> {
                logger.error("Unknown message type - $message")
                val exception = IllegalArgumentException("Unknown message type - %s".format(message))
                connectionDelegate.onError(exception)
            }
        }
    }

    @Suppress("OverridingDeprecatedMember")
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        // unwrap CodecException if needed
        when (cause) {
            is CodecException -> connectionDelegate.onError(cause.cause!!)
            else -> connectionDelegate.onError(cause)
        }
    }

    @Suppress("RedundantUnitReturnType")
    override fun channelInactive(ctx: ChannelHandlerContext) {
        logger.info("Connection disconnected - {}", ctx.channel().remoteAddress())
    }

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        this.currentContext = ctx
    }

    fun write(message: ClientMessage) {
        this.currentContext!!.writeAndFlush(message).onFailure(executionContext) { e: Throwable ->
            connectionDelegate.onError(e)
        }
    }
}
