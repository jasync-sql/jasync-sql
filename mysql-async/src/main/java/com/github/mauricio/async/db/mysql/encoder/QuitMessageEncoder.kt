
package com.github.mauricio.async.db.mysql.encoder

import com.github.jasync.sql.db.util.ByteBufferUtils
import com.github.mauricio.async.db.mysql.message.client.ClientMessage
import io.netty.buffer.ByteBuf

object QuitMessageEncoder : MessageEncoder {

  override fun encode(message: ClientMessage): ByteBuf {
    val buffer = ByteBufferUtils.packetBuffer(5)
    buffer.writeByte( ClientMessage.Quit )
    return buffer
  }

}
