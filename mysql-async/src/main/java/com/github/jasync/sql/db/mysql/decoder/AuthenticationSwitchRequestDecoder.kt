package com.github.jasync.sql.db.mysql.decoder

import com.github.jasync.sql.db.mysql.message.server.AuthenticationSwitchRequest
import com.github.jasync.sql.db.mysql.message.server.ServerMessage
import com.github.jasync.sql.db.util.readCString
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import java.nio.charset.Charset

class AuthenticationSwitchRequestDecoder(val charset: Charset) : MessageDecoder {
    override fun decode(buffer: ByteBuf): ServerMessage {
        val method = buffer.readCString(charset)
        val bytes: Int = buffer.readableBytes()
        val terminal = 0.toByte()
        val seed = if (bytes > 0 && buffer.getByte(buffer.writerIndex() - 1) == terminal) ByteBufUtil.getBytes(
            buffer,
            buffer.readerIndex(),
            bytes - 1
        ) else ByteBufUtil.getBytes(buffer)
        buffer.skipBytes(bytes)
        return AuthenticationSwitchRequest(method, seed)
    }
}
