package com.github.jasync.sql.db.postgresql.encoders

import com.github.jasync.sql.db.postgresql.messages.frontend.ClientMessage
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

object CloseMessageEncoder : Encoder {

  override fun encode(message: ClientMessage): ByteBuf {
    val buffer = Unpooled.buffer(5)
    buffer.writeByte('X'.toInt())
    buffer.writeInt(4)
    return buffer
  }

}
