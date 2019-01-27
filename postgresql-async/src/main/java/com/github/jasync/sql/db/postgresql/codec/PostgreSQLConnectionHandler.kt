package com.github.jasync.sql.db.postgresql.codec

import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.SSLConfiguration
import com.github.jasync.sql.db.column.ColumnEncoderRegistry
import com.github.jasync.sql.db.postgresql.exceptions.QueryMustNotBeNullOrEmptyException
import com.github.jasync.sql.db.postgresql.messages.backend.AuthenticationMessage
import com.github.jasync.sql.db.postgresql.messages.backend.CommandCompleteMessage
import com.github.jasync.sql.db.postgresql.messages.backend.DataRowMessage
import com.github.jasync.sql.db.postgresql.messages.backend.ErrorMessage
import com.github.jasync.sql.db.postgresql.messages.backend.NotificationResponse
import com.github.jasync.sql.db.postgresql.messages.backend.ParameterStatusMessage
import com.github.jasync.sql.db.postgresql.messages.backend.ProcessData
import com.github.jasync.sql.db.postgresql.messages.backend.RowDescriptionMessage
import com.github.jasync.sql.db.postgresql.messages.backend.SSLResponseMessage
import com.github.jasync.sql.db.postgresql.messages.backend.ServerMessage
import com.github.jasync.sql.db.postgresql.messages.frontend.ClientMessage
import com.github.jasync.sql.db.postgresql.messages.frontend.CloseMessage
import com.github.jasync.sql.db.postgresql.messages.frontend.SSLRequestMessage
import com.github.jasync.sql.db.postgresql.messages.frontend.StartupMessage
import com.github.jasync.sql.db.util.ExecutorServiceUtils
import com.github.jasync.sql.db.util.Failure
import com.github.jasync.sql.db.util.NettyUtils
import com.github.jasync.sql.db.util.Success
import com.github.jasync.sql.db.util.failed
import com.github.jasync.sql.db.util.onCompleteAsync
import com.github.jasync.sql.db.util.onFailure
import com.github.jasync.sql.db.util.success
import com.github.jasync.sql.db.util.toCompletableFuture
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.CodecException
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.SslHandler
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import mu.KotlinLogging
import java.io.FileInputStream
import java.net.InetSocketAddress
import java.security.KeyStore
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import javax.net.ssl.TrustManagerFactory

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
        "extra_float_digits" to "2"
    )

    //  private val executionContext: Executor = ExecutorServiceUtils.CommonPool
    private val bootstrap = Bootstrap()
    private val connectionFuture = CompletableFuture<PostgreSQLConnectionHandler>()
    private val disconnectionPromise = CompletableFuture<PostgreSQLConnectionHandler>()
    private var processData: ProcessData? = null
    private var currentContext: ChannelHandlerContext? = null

    fun connect(): CompletableFuture<PostgreSQLConnectionHandler> {
        this.bootstrap.group(this.group)
        this.bootstrap.channel(NettyUtils.getSocketChannelClass(this.group))
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
        this.bootstrap.connect(InetSocketAddress(configuration.host, configuration.port))
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
                        is Failure -> this.disconnectionPromise.failed(ty1.exception)
                    }
                }
        }

        return this.disconnectionPromise
    }

    fun isConnected(): Boolean {
        return this.currentContext?.channel()?.isActive ?: false
    }

    @Suppress("RedundantUnitReturnType")
    override fun channelActive(ctx: ChannelHandlerContext): Unit {
        if (configuration.ssl.mode == SSLConfiguration.Mode.Disable)
            ctx.writeAndFlush(StartupMessage(this.properties))
        else
            ctx.writeAndFlush(SSLRequestMessage)
    }

    override fun channelRead0(ctx: ChannelHandlerContext?, message: Any) {
        when (message) {
            SSLResponseMessage(true) -> {
                val ctxBuilder = SslContextBuilder.forClient()
                if (configuration.ssl.mode >= SSLConfiguration.Mode.VerifyCA) {
                    if (configuration.ssl.rootCert == null) {
                        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                        val ks = KeyStore.getInstance(KeyStore.getDefaultType())
                        val cacerts = FileInputStream(System.getProperty("java.home") + "/lib/security/cacerts")
                        cacerts.use { ks.load(it, "changeit".toCharArray()) }
                        tmf.init(ks)
                        ctxBuilder.trustManager(tmf)
                    } else {
                        ctxBuilder.trustManager(configuration.ssl.rootCert)
                    }
                } else {
                    ctxBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE)
                }
                val sslContext = ctxBuilder.build()
                val sslEngine = sslContext.newEngine(ctx!!.alloc(), configuration.host, configuration.port)
                if (configuration.ssl.mode >= SSLConfiguration.Mode.VerifyFull) {
                    val sslParams = sslEngine.sslParameters
                    sslParams.endpointIdentificationAlgorithm = "HTTPS"
                    sslEngine.sslParameters = sslParams
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
    override fun channelInactive(ctx: ChannelHandlerContext): Unit {
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
