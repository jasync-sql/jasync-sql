package com.github.jasync.sql.db.postgresql.encoders

import com.github.jasync.sql.db.postgresql.messages.frontend.StartupMessage
import com.github.jasync.sql.db.util.ByteBufferUtils
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import mu.KotlinLogging
import java.nio.charset.Charset

private val logger = KotlinLogging.logger {}

class StartupMessageEncoder(val charset: Charset) {

    fun encode(startup: StartupMessage): ByteBuf {

        val buffer = Unpooled.buffer()
        buffer.writeInt(0)
        buffer.writeShort(3)
        buffer.writeShort(0)

        startup.parameters.forEach { pair ->
            if (pair.second != null) {
                ByteBufferUtils.writeCString(pair.first, buffer, charset)
                ByteBufferUtils.writeCString(pair.second.toString(), buffer, charset)
            } else {
                logger.info { "skip null parameter: $pair" }
            }
        }

        buffer.writeByte(0)
        val index = buffer.writerIndex()
        buffer.markWriterIndex()
        buffer.writerIndex(0)
        buffer.writeInt(index)
        buffer.resetWriterIndex()

        return buffer
    }
}
