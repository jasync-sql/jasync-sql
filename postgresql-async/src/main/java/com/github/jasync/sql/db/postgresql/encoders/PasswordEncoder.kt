package com.github.jasync.sql.db.postgresql.encoders

import com.github.jasync.sql.db.postgresql.messages.backend.ServerMessage
import com.github.jasync.sql.db.postgresql.messages.frontend.ClientMessage
import com.github.jasync.sql.db.postgresql.messages.frontend.PasswordMessage
import com.github.jasync.sql.db.postgresql.messages.frontend.SASLInitialResponse
import com.github.jasync.sql.db.postgresql.messages.frontend.SASLResponse
import com.github.jasync.sql.db.postgresql.util.PasswordHelper
import com.github.jasync.sql.db.util.ByteBufferUtils
import com.github.jasync.sql.db.util.XXX
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import java.nio.charset.Charset

class PasswordEncoder(val charset: Charset) : Encoder {

    override fun encode(message: ClientMessage): ByteBuf {
        require(message.kind == ServerMessage.PasswordMessage)

        return when (message) {
            is PasswordMessage -> encodeSimplePassword(message)
            is SASLInitialResponse -> encodeSASLInitialResponse(message)
            is SASLResponse -> encodeSASLResponse(message)
            else -> XXX("Unknown password message type ${message.javaClass}")
        }
    }

    private fun encodeSimplePassword(message: PasswordMessage): ByteBuf {
        val password = if (message.salt == null) {
            message.password.toByteArray(charset)
        } else {
            PasswordHelper.encode(message.username, message.password, message.salt, charset)
        }

        return Unpooled.buffer(1 + 4 + password.size + 1).apply {
            writeByte(ServerMessage.PasswordMessage)
            writeInt(0)
            writeBytes(password)
            writeByte(0)
            ByteBufferUtils.writeLength(this)
        }
    }

    private fun encodeSASLInitialResponse(message: SASLInitialResponse): ByteBuf =
        Unpooled.buffer(1 + 4 + message.mechanism.length + 1 + 4 + message.saslData.length).apply {
            writeByte(ServerMessage.PasswordMessage)
            writeInt(0)
            writeBytes(message.mechanism.toByteArray(charset))
            writeByte(0)
            writeInt(message.saslData.length)
            writeBytes(message.saslData.toByteArray(charset))
            ByteBufferUtils.writeLength(this)
        }

    private fun encodeSASLResponse(message: SASLResponse): ByteBuf =
        Unpooled.buffer(1 + 4 + message.saslData.length).apply {
            writeByte(ServerMessage.PasswordMessage)
            writeInt(0)
            writeBytes(message.saslData.toByteArray(charset))
            ByteBufferUtils.writeLength(this)
        }
}
