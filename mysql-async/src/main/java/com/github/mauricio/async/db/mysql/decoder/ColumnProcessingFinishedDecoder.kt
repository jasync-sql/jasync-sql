
package com.github.mauricio.async.db.mysql.decoder

import io.netty.buffer.ByteBuf
import com.github.mauricio.async.db.mysql.message.server.ColumnProcessingFinishedMessage
import com.github.mauricio.async.db.mysql.message.server.ServerMessage

object ColumnProcessingFinishedDecoder : MessageDecoder {

  override fun decode(buffer: ByteBuf): ServerMessage {
    return ColumnProcessingFinishedMessage( EOFMessageDecoder.decode(buffer) )
  }

}
