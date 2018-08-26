
package com.github.mauricio.async.db.mysql.decoder

import com.github.jasync.sql.db.util.readFixedString
import com.github.jasync.sql.db.util.readUntilEOF
import com.github.mauricio.async.db.mysql.message.server.ErrorMessage
import com.github.mauricio.async.db.mysql.message.server.ServerMessage
import io.netty.buffer.ByteBuf
import java.nio.charset.Charset

class ErrorDecoder( val charset : Charset ) : MessageDecoder {

   override fun decode(buffer: ByteBuf): ServerMessage {

     return ErrorMessage(
      buffer.readShort().toInt(),
      buffer.readFixedString( 6, charset ),
      buffer.readUntilEOF(charset)
    )

  }

}
