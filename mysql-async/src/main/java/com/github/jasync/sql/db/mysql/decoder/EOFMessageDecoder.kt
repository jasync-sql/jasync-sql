
package com.github.jasync.sql.db.mysql.decoder

import io.netty.buffer.ByteBuf
import com.github.jasync.sql.db.mysql.message.server.EOFMessage

object EOFMessageDecoder : MessageDecoder {

   override fun decode(buffer: ByteBuf): EOFMessage {
    return EOFMessage(
      buffer.readUnsignedShort(),
      buffer.readUnsignedShort() )
  }

}
