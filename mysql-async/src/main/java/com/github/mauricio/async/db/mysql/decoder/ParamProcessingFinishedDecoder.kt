
package com.github.mauricio.async.db.mysql.decoder

import io.netty.buffer.ByteBuf
import com.github.mauricio.async.db.mysql.message.server.ParamProcessingFinishedMessage
import com.github.mauricio.async.db.mysql.message.server.ServerMessage

object ParamProcessingFinishedDecoder : MessageDecoder {

   override fun decode(buffer: ByteBuf): ServerMessage {
     return ParamProcessingFinishedMessage(EOFMessageDecoder.decode(buffer))
  }

}
