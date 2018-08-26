
package com.github.mauricio.async.db.mysql.decoder

import io.netty.buffer.ByteBuf
import com.github.mauricio.async.db.mysql.message.server.EOFMessage

object EOFMessageDecoder : MessageDecoder {

   override fun decode(buffer: ByteBuf): EOFMessage {
    return EOFMessage(
      buffer.readUnsignedShort(),
      buffer.readUnsignedShort() )
  }

}
