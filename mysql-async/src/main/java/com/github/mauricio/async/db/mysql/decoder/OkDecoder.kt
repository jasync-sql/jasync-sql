
package com.github.mauricio.async.db.mysql.decoder

import com.github.jasync.sql.db.util.readBinaryLength
import com.github.jasync.sql.db.util.readUntilEOF
import com.github.mauricio.async.db.mysql.message.server.OkMessage
import com.github.mauricio.async.db.mysql.message.server.ServerMessage
import io.netty.buffer.ByteBuf
import java.nio.charset.Charset

class OkDecoder( val charset : Charset ) : MessageDecoder {

   override fun decode(buffer: ByteBuf): ServerMessage {

    return OkMessage(
      buffer.readBinaryLength(),
      buffer.readBinaryLength(),
      buffer.readShort().toInt(),
      buffer.readShort().toInt(),
      buffer.readUntilEOF(charset)
    )

  }

}
