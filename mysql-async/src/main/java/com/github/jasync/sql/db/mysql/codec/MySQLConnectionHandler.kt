package com.github.jasync.sql.db.mysql.codec

import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.exceptions.DatabaseException
import com.github.jasync.sql.db.general.MutableResultSet
import com.github.jasync.sql.db.mysql.binary.BinaryRowDecoder
import com.github.jasync.sql.db.mysql.encoder.auth.AuthenticationMethod
import com.github.jasync.sql.db.mysql.message.client.AuthenticationSwitchResponse
import com.github.jasync.sql.db.mysql.message.client.CapabilityRequestMessage
import com.github.jasync.sql.db.mysql.message.client.CloseStatementMessage
import com.github.jasync.sql.db.mysql.message.client.HandshakeResponseMessage
import com.github.jasync.sql.db.mysql.message.client.PreparedStatementExecuteMessage
import com.github.jasync.sql.db.mysql.message.client.PreparedStatementPrepareMessage
import com.github.jasync.sql.db.mysql.message.client.QueryMessage
import com.github.jasync.sql.db.mysql.message.client.QuitMessage
import com.github.jasync.sql.db.mysql.message.client.SendLongDataMessage
import com.github.jasync.sql.db.mysql.message.server.AuthMoreDataMessage
import com.github.jasync.sql.db.mysql.message.server.AuthenticationSwitchRequest
import com.github.jasync.sql.db.mysql.message.server.BinaryRowMessage
import com.github.jasync.sql.db.mysql.message.server.ColumnDefinitionMessage
import com.github.jasync.sql.db.mysql.message.server.EOFMessage
import com.github.jasync.sql.db.mysql.message.server.ErrorMessage
import com.github.jasync.sql.db.mysql.message.server.HandshakeMessage
import com.github.jasync.sql.db.mysql.message.server.OkMessage
import com.github.jasync.sql.db.mysql.message.server.PreparedStatementPrepareResponse
import com.github.jasync.sql.db.mysql.message.server.ResultSetRowMessage
import com.github.jasync.sql.db.mysql.message.server.ServerMessage
import com.github.jasync.sql.db.mysql.util.CapabilityFlag
import com.github.jasync.sql.db.mysql.util.CharsetMapper
import com.github.jasync.sql.db.util.ExecutorServiceUtils
import com.github.jasync.sql.db.util.FP
import com.github.jasync.sql.db.util.NettyUtils
import com.github.jasync.sql.db.util.XXX
import com.github.jasync.sql.db.util.failed
import com.github.jasync.sql.db.util.flatMapAsync
import com.github.jasync.sql.db.util.head
import com.github.jasync.sql.db.util.installOnFuture
import com.github.jasync.sql.db.util.length
import com.github.jasync.sql.db.util.onFailure
import com.github.jasync.sql.db.util.tail
import com.github.jasync.sql.db.util.toCompletableFuture
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.CodecException
import mu.KotlinLogging
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

private val logger = KotlinLogging.logger {}

class MySQLConnectionHandler(
    val configuration: Configuration,
    charsetMapper: CharsetMapper,
    private val handlerDelegate: MySQLHandlerDelegate,
    private val group: EventLoopGroup,
    private val executionContext: Executor = ExecutorServiceUtils.CommonPool,
    private val connectionId: String
) : SimpleChannelInboundHandler<Any>() {

    private val bootstrap = Bootstrap().group(this.group)
    private val connectionPromise = CompletableFuture<MySQLConnectionHandler>()
    private val decoder = MySQLFrameDecoder(configuration.charset, connectionId)
    private val encoder = MySQLOneToOneEncoder(configuration.charset, charsetMapper)
    private val sendLongDataEncoder = SendLongDataEncoder()
    private val currentColumns = mutableListOf<ColumnDefinitionMessage>()
    private val parsedStatements = HashMap<String, PreparedStatementHolder>()
    private val binaryRowDecoder = BinaryRowDecoder()

    private var sslEstablished: Boolean = false
    private var currentPreparedStatementHolder: PreparedStatementHolder? = null
    private var currentPreparedStatement: PreparedStatement? = null
    private var currentQuery: MutableResultSet<ColumnDefinitionMessage>? = null
    private var currentContext: ChannelHandlerContext? = null
    private var currentQueryString: String? = null
    private var isPreparedStatement: Boolean? = null

    fun connect(): CompletableFuture<MySQLConnectionHandler> {
        val socketChannelAddress = NettyUtils.getSocketChannelClassAndSocketAddress(
            this.group,
            configuration
        )
        this.bootstrap.channel(socketChannelAddress.socketChannelClass)
        this.bootstrap.handler(object : ChannelInitializer<Channel>() {

            override fun initChannel(channel: Channel) {
                channel.pipeline().addLast(
                    decoder,
                    encoder,
                    sendLongDataEncoder,
                    this@MySQLConnectionHandler
                )
            }
        })

        this.bootstrap.option(ChannelOption.SO_KEEPALIVE, true)
        this.bootstrap.option(ChannelOption.ALLOCATOR, LittleEndianByteBufAllocator.INSTANCE)
        this.bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.configuration.connectionTimeout)

        val channelFuture: ChannelFuture = this.bootstrap.connect(socketChannelAddress.socketAddress)
        channelFuture.onFailure(executionContext) { exception ->
            this.connectionPromise.completeExceptionally(exception)
        }

        return this.connectionPromise
    }

    override fun channelRead0(ctx: ChannelHandlerContext, message: Any) {
        when (message) {
            is ServerMessage -> {
                when (message.kind) {
                    ServerMessage.ServerProtocolVersion -> {
                        handlerDelegate.onHandshake(message as HandshakeMessage)
                    }
                    ServerMessage.Ok -> {
                        this.clearQueryState()
                        handlerDelegate.onOk(message as OkMessage)
                    }
                    ServerMessage.Error -> {
                        this.clearQueryState()
                        handlerDelegate.onError(message as ErrorMessage)
                    }
                    ServerMessage.EOF -> {
                        this.handleEOF(message)
                    }
                    ServerMessage.AuthMoreData -> {
                        val m = message as AuthMoreDataMessage

                        if (!m.isSuccess()) {
                            if (!sslEstablished) {
                                throw IllegalStateException(
                                    "Full authentication mode for ${AuthenticationMethod.CachingSha2} requires SSL"
                                )
                            }

                            val request = AuthenticationSwitchRequest(AuthenticationMethod.CachingSha2, null)
                            handlerDelegate.switchAuthentication(request)
                        }
                    }
                    ServerMessage.ColumnDefinition -> {
                        val m = message as ColumnDefinitionMessage

                        this.currentPreparedStatementHolder?.let {
                            if (it.needsAny()) {
                                it.add(m)
                            }
                        }

                        this.currentColumns += message
                    }
                    ServerMessage.ColumnDefinitionFinished -> {
                        this.onColumnDefinitionFinished()
                    }
                    ServerMessage.PreparedStatementPrepareResponse -> {
                        this.onPreparedStatementPrepareResponse(message as PreparedStatementPrepareResponse)
                    }
                    ServerMessage.Row -> {
                        val rsrMessage = message as ResultSetRowMessage
                        val items = Array(rsrMessage.size) {
                            if (rsrMessage[it] == null) {
                                null
                            } else {
                                if (this.currentQuery == null) {
                                    throw NullPointerException("currentQuery is null")
                                }
                                val columnDescription: ColumnDefinitionMessage = this.currentQuery!!.columnTypes[it]
                                val buf = rsrMessage[it]!!
                                try {
                                    columnDescription.textDecoder.decode(
                                        columnDescription,
                                        buf,
                                        configuration.charset
                                    )
                                } finally {
                                    buf.release()
                                }
                            }
                        }

                        this.currentQuery!!.addRow(items)
                    }
                    ServerMessage.BinaryRow -> {
                        val m = message as BinaryRowMessage
                        try {
                            val decodedRow = this.binaryRowDecoder.decode(m.buffer, this.currentColumns)
                            this.currentQuery!!.addRow(decodedRow)
                        } finally {
                            m.buffer.release()
                        }
                    }
                    ServerMessage.ParamProcessingFinished -> {
                    }
                    ServerMessage.ParamAndColumnProcessingFinished -> {
                        this.onColumnDefinitionFinished()
                    }
                }
            }
        }
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        logger.trace { "[connectionId:$connectionId] - Channel became active" }
        handlerDelegate.connected(ctx)
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        logger.trace { "[connectionId:$connectionId] - Channel became inactive" }
    }

    override fun channelRegistered(ctx: ChannelHandlerContext) {
        logger.trace { "[connectionId:$connectionId] - channelRegistered" }
        super.channelRegistered(ctx)
    }

    override fun channelUnregistered(ctx: ChannelHandlerContext) {
        logger.trace { "[connectionId:$connectionId] - channelUnregistered" }
        handlerDelegate.unregistered()
        super.channelUnregistered(ctx)
    }

    @Suppress("OverridingDeprecatedMember")
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        // unwrap CodecException if needed
        when (cause) {
            is CodecException -> handleException(cause.cause ?: cause)
            else -> handleException(cause)
        }
    }

    private fun handleException(cause: Throwable) {
        if (!this.connectionPromise.isDone) {
            this.connectionPromise.failed(cause)
        }
        handlerDelegate.exceptionCaught(cause)
    }

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        this.currentContext = ctx
    }

    private fun write(message: QueryMessage): ChannelFuture {
        this.decoder.queryProcessStarted()
        return writeAndHandleError(message)
    }

    fun sendQuery(query: String) {
        this.isPreparedStatement = false
        this.currentQueryString = query
        this.write(QueryMessage(query))
    }

    fun sendPreparedStatement(query: String, values: List<Any?>): CompletableFuture<ChannelFuture> {
        val preparedStatement = PreparedStatement(query, values)
        this.isPreparedStatement = true
        this.currentQueryString = query
        this.currentColumns.clear()

        this.currentPreparedStatement = preparedStatement

        val item = this.parsedStatements[preparedStatement.statement]
        return when {
            item != null -> {
                this.executePreparedStatement(
                    item.statementId(),
                    item.columns.size,
                    preparedStatement.values,
                    item.parameters
                )
            }
            else -> {
                decoder.preparedStatementPrepareStarted()
                writeAndHandleError(PreparedStatementPrepareMessage(preparedStatement.statement)).toCompletableFuture()
            }
        }
    }

    fun closePreparedStatement(query: String): CompletableFuture<Boolean> {
        val statement = this.parsedStatements[query]
        return if (statement != null) {
            this.parsedStatements.remove(query)
            this.writeAndHandleError(CloseStatementMessage(statement.statementId()))
            FP.successful(true)
        } else {
            FP.successful(false)
        }
    }

    fun write(message: CapabilityRequestMessage): ChannelFuture = writeAndHandleError(message)

    fun write(message: HandshakeResponseMessage): ChannelFuture {
        sslEstablished = message.header.flags.contains(CapabilityFlag.CLIENT_SSL)
        decoder.hasDoneHandshake = true
        return writeAndHandleError(message)
    }

    fun write(message: AuthenticationSwitchResponse): ChannelFuture = writeAndHandleError(message)

    fun sendQuitMessage(): CompletableFuture<Channel> {
        val future = CompletableFuture<Channel>()
        val channel = this.currentContext!!.channel()
        channel.eventLoop().execute {
            this.clearQueryState()
            if (channel.isActive) {
                writeAndHandleError(QuitMessage.Instance).installOnFuture(future)
            } else {
                future.complete(channel)
            }
        }
        return future
    }

    fun closeChannel(): ChannelFuture {
        return this.currentContext!!.channel().close()
    }

    private fun clearQueryState() {
        this.currentColumns.clear()
        this.currentQuery = null
        this.isPreparedStatement = null
        this.currentQueryString = null
    }

    fun isConnected(): Boolean {
        return this.currentContext?.channel()?.isActive ?: false
    }

    private fun executePreparedStatement(
        statementId: ByteArray,
        columnsCount: Int,
        values: List<Any?>,
        parameters: List<ColumnDefinitionMessage>
    ): CompletableFuture<ChannelFuture> {
        decoder.preparedStatementExecuteStarted(columnsCount, parameters.size)
        this.currentColumns.clear()
        val (longValues1, nonLongIndicesOpt1) = values.mapIndexed { index, any -> index to any }
            .partition { (_, any) -> any != null && isLong(any) }
        val nonLongIndices: List<Int> = nonLongIndicesOpt1.map { it.first }
        val longValues: List<Pair<Int, Any>> =
            longValues1.mapNotNull { if (it.second == null) null else it.first to it.second!! }

        return if (longValues.isNotEmpty()) {
            val (firstIndex, firstValue) = longValues.head
            var channelFuture: CompletableFuture<ChannelFuture> = sendLongParameter(statementId, firstIndex, firstValue)
            longValues.tail.forEach { (index, value) ->
                channelFuture = channelFuture.flatMapAsync(executionContext) {
                    sendLongParameter(statementId, index, value)
                }
            }
            channelFuture.toCompletableFuture().flatMapAsync(executionContext) {
                writeAndHandleError(
                    PreparedStatementExecuteMessage(
                        statementId,
                        values,
                        nonLongIndices.toSet(),
                        parameters
                    )
                ).toCompletableFuture()
            }
        } else {
            writeAndHandleError(
                PreparedStatementExecuteMessage(
                    statementId,
                    values,
                    nonLongIndices.toSet(),
                    parameters
                )
            ).toCompletableFuture()
        }
    }

    private fun isLong(value: Any): Boolean {
        return when (value) {
            is ByteArray -> value.length > SendLongDataEncoder.LONG_THRESHOLD
            is ByteBuffer -> value.remaining() > SendLongDataEncoder.LONG_THRESHOLD
            is ByteBuf -> value.readableBytes() > SendLongDataEncoder.LONG_THRESHOLD
            else -> false
        }
    }

    private fun sendLongParameter(
        statementId: ByteArray,
        index: Int,
        longValue: Any
    ): CompletableFuture<ChannelFuture> {
        return when (longValue) {
            is ByteArray ->
                sendBuffer(Unpooled.wrappedBuffer(longValue), statementId, index)

            is ByteBuffer ->
                sendBuffer(Unpooled.wrappedBuffer(longValue), statementId, index)

            is ByteBuf ->
                sendBuffer(longValue, statementId, index)
            else -> XXX("no handle for ${longValue::class.java}")
        }.toCompletableFuture()
    }

    private fun sendBuffer(buffer: ByteBuf, statementId: ByteArray, paramId: Int): ChannelFuture {
        return writeAndHandleError(SendLongDataMessage(statementId, buffer, paramId))
    }

    private fun onPreparedStatementPrepareResponse(message: PreparedStatementPrepareResponse) {
        this.currentPreparedStatementHolder =
            PreparedStatementHolder(this.currentPreparedStatement!!.statement, message)
    }

    private fun onColumnDefinitionFinished() {
        logger.trace { "[connectionId:$connectionId] - onColumnDefinitionFinished()" }

        val columns =
            this.currentPreparedStatementHolder?.columns ?: this.currentColumns

        this.currentQuery = MutableResultSet(columns.toList())

        this.currentPreparedStatementHolder?.let {
            this.parsedStatements[it.statement] = it
            this.executePreparedStatement(
                it.statementId(),
                it.columns.size,
                this.currentPreparedStatement!!.values,
                it.parameters
            )
            this.currentPreparedStatementHolder = null
            this.currentPreparedStatement = null
        }
    }

    private fun writeAndHandleError(message: Any): ChannelFuture {
        return if (currentContext?.channel()?.isActive == true) {
            val res: ChannelFuture = currentContext!!.writeAndFlush(message)

            res.onFailure(executionContext) { e: Throwable ->
                handleException(e)
            }

            res
        } else {
            val error = DatabaseException("This channel is not active and can't take messages")
            handleException(error)
            currentContext!!.channel().newFailedFuture(error)
        }
    }

    private fun handleEOF(m: ServerMessage) {
        when (m) {
            is EOFMessage -> {
                val resultSet = this.currentQuery
                this.clearQueryState()

                if (resultSet != null) {
                    handlerDelegate.onResultSet(resultSet, m)
                } else {
                    handlerDelegate.onEOF(m)
                }
            }
            is AuthenticationSwitchRequest -> {
                handlerDelegate.switchAuthentication(m)
            }
        }
    }
}
