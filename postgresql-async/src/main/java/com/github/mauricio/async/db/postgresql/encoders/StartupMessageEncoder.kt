package com.github.mauricio.async.db.postgresql.encoders

import com.github.jasync.sql.db.util.ByteBufferUtils
import com.github.mauricio.async.db.postgresql.messages.frontend.StartupMessage
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import java.nio.charset.Charset


class StartupMessageEncoder(val charset: Charset) {

  //private val log = Log.getByName("StartupMessageEncoder")

  fun encode(startup: StartupMessage): ByteBuf {

    val buffer = Unpooled.buffer()
    buffer.writeInt(0)
    buffer.writeShort(3)
    buffer.writeShort(0)

    startup.parameters.forEach { pair ->
      ByteBufferUtils.writeCString(pair.first, buffer, charset)
      ByteBufferUtils.writeCString(pair.second.toString(), buffer, charset)
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