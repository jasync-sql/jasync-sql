
package com.github.jasync.sql.db.mysql.decoder

import io.netty.buffer.ByteBuf
import com.github.jasync.sql.db.mysql.message.server.ParamProcessingFinishedMessage
import com.github.jasync.sql.db.mysql.message.server.ServerMessage

object ParamProcessingFinishedDecoder : MessageDecoder {

   override fun decode(buffer: ByteBuf): ServerMessage {
     return ParamProcessingFinishedMessage(EOFMessageDecoder.decode(buffer))
  }

}
