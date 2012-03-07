package com.github.mauricio.postgresql.encoders

import com.github.mauricio.postgresql.messages.QueryMessage
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}
import com.github.mauricio.postgresql.{FrontendMessage, ChannelUtils, CharsetHelper, Message}

/**
 * User: Maurício Linhares
 * Date: 3/3/12
 * Time: 8:32 PM
 */

object QueryMessageEncoder extends Encoder {

  override def encode(message: FrontendMessage): ChannelBuffer = {

    val m = message.asInstanceOf[QueryMessage]

    val buffer = ChannelBuffers.dynamicBuffer()
    buffer.writeByte( Message.Query )
    buffer.writeInt(0)
    buffer.writeBytes( CharsetHelper.toBytes( m.query ) )
    buffer.writeByte(0)

    ChannelUtils.writeLength(  buffer )

    buffer
  }

}
