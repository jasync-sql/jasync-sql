package com.github.jasync.sql.db.postgresql.encoders

import com.github.jasync.sql.db.postgresql.messages.backend.ServerMessage
import com.github.jasync.sql.db.postgresql.messages.frontend.ClientMessage
import com.github.jasync.sql.db.postgresql.messages.frontend.QueryMessage
import com.github.jasync.sql.db.util.ByteBufferUtils
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import mu.KotlinLogging
import java.nio.charset.Charset

private val logger = KotlinLogging.logger {}

class QueryMessageEncoder(val charset: Charset) : Encoder {

    override fun encode(message: ClientMessage): ByteBuf {

        val m = message as QueryMessage
        logger.debug("Executing direct query ({})", m.query)

        val buffer = Unpooled.buffer()
        buffer.writeByte(ServerMessage.Query)
        buffer.writeInt(0)
        ByteBufferUtils.writeCString(m.query, buffer, charset)
        ByteBufferUtils.writeLength(buffer)

        return buffer
    }
}
