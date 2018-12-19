package com.github.jasync.sql.db.mysql.codec

import com.github.jasync.sql.db.exceptions.EncoderNotAvailableException
import com.github.jasync.sql.db.mysql.binary.BinaryRowEncoder
import com.github.jasync.sql.db.mysql.encoder.AuthenticationSwitchResponseEncoder
import com.github.jasync.sql.db.mysql.encoder.HandshakeResponseEncoder
import com.github.jasync.sql.db.mysql.encoder.PreparedStatementExecuteEncoder
import com.github.jasync.sql.db.mysql.encoder.PreparedStatementPrepareEncoder
import com.github.jasync.sql.db.mysql.encoder.QueryMessageEncoder
import com.github.jasync.sql.db.mysql.encoder.QuitMessageEncoder
import com.github.jasync.sql.db.mysql.message.client.ClientMessage
import com.github.jasync.sql.db.mysql.util.CharsetMapper
import com.github.jasync.sql.db.util.BufferDumper
import com.github.jasync.sql.db.util.ByteBufferUtils
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageEncoder
import mu.KotlinLogging
import java.nio.charset.Charset


class MySQLOneToOneEncoder(charset: Charset, charsetMapper: CharsetMapper) :
    MessageToMessageEncoder<ClientMessage>(ClientMessage::class.java) {


    private val handshakeResponseEncoder = HandshakeResponseEncoder(charset, charsetMapper)
    private val queryEncoder = QueryMessageEncoder(charset)
    private val rowEncoder = BinaryRowEncoder(charset)
    private val prepareEncoder = PreparedStatementPrepareEncoder(charset)
    private val executeEncoder = PreparedStatementExecuteEncoder(rowEncoder)
    private val authenticationSwitchEncoder = AuthenticationSwitchResponseEncoder(charset)

    private var sequence = 1

    override fun encode(ctx: ChannelHandlerContext, message: ClientMessage, out: MutableList<Any>): Unit {
        val encoder = when (message.kind) {
            ClientMessage.ClientProtocolVersion -> this.handshakeResponseEncoder
            ClientMessage.Quit -> {
                sequence = 0
                QuitMessageEncoder
            }
            ClientMessage.Query -> {
                sequence = 0
                this.queryEncoder
            }
            ClientMessage.PreparedStatementExecute -> {
                sequence = 0
                this.executeEncoder
            }
            ClientMessage.PreparedStatementPrepare -> {
                sequence = 0
                this.prepareEncoder
            }
            ClientMessage.AuthSwitchResponse -> {
                sequence += 1
                this.authenticationSwitchEncoder
            }
            else -> throw EncoderNotAvailableException(message)
        }

        val result: ByteBuf = encoder.encode(message)

        ByteBufferUtils.writePacketLength(result, sequence)

        sequence += 1

        if (logger.isTraceEnabled) {
            logger.trace("Writing message ${message::class.java.name} - \n${BufferDumper.dumpAsHex(result)}")
        }

        out.add(result)
    }

}

private val logger = KotlinLogging.logger {}
