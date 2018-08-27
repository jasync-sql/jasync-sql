
package com.github.jasync.sql.db.mysql.decoder

import com.github.jasync.sql.db.util.readBinaryLength
import com.github.jasync.sql.db.mysql.message.server.ResultSetRowMessage
import com.github.jasync.sql.db.mysql.message.server.ServerMessage
import io.netty.buffer.ByteBuf
import java.nio.charset.Charset

//TODO check occurences
private   val NULL = 0xfb.toShort()


class ResultSetRowDecoder(charset: Charset) : MessageDecoder {


   override fun decode(buffer: ByteBuf): ServerMessage {
    val row = ResultSetRowMessage()

    while (buffer.isReadable) {
      if (buffer.getUnsignedByte(buffer.readerIndex()) == NULL) {
        buffer.readByte()
        row.add(null)
      } else {
        val length = buffer.readBinaryLength().toInt()
        row.add(buffer.readBytes(length))
      }
    }

    return row
  }
}
