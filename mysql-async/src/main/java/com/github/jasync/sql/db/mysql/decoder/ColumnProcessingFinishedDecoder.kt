
package com.github.jasync.sql.db.mysql.decoder

import io.netty.buffer.ByteBuf
import com.github.jasync.sql.db.mysql.message.server.ColumnProcessingFinishedMessage
import com.github.jasync.sql.db.mysql.message.server.ServerMessage

object ColumnProcessingFinishedDecoder : MessageDecoder {

  override fun decode(buffer: ByteBuf): ServerMessage {
    return ColumnProcessingFinishedMessage( EOFMessageDecoder.decode(buffer) )
  }

}
