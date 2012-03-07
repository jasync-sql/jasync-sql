package com.github.mauricio.postgresql.encoders

import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}
import com.github.mauricio.postgresql.messages.ParseMessage
import com.github.mauricio.postgresql.{FrontendMessage, CharsetHelper}


/**
 * User: Maurício Linhares
 * Date: 3/7/12
 * Time: 9:20 AM
 */

object ParseMessageEncoder extends Encoder {
  override def encode(message: FrontendMessage): ChannelBuffer = {

    val m = message.asInstanceOf[ParseMessage]

    val queryBytes = CharsetHelper.toBytes(m.command)
    val columnCount = m.parameterTypes.size

    val buffer = ChannelBuffers.dynamicBuffer( 1 + 4 + (queryBytes.length + 2) + 2 + ( columnCount * 4 ) + 4 )

    buffer.writeByte(m.kind)
    buffer.writeInt(0)
    buffer.writeBytes(queryBytes)
    buffer.writeByte(0)
    buffer.writeBytes(queryBytes)
    buffer.writeByte(0)
    buffer.writeShort(columnCount)
    m.parameterTypes.foreach {
      kind =>
        buffer.writeInt(kind)
    }

    buffer
  }
}
