package com.github.mauricio.postgresql.encoders

import com.github.mauricio.postgresql.ChannelUtils
import com.github.mauricio.postgresql.messages.backend.Message
import com.github.mauricio.postgresql.messages.frontend.{FrontendMessage, QueryMessage}
import java.nio.charset.Charset
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}

/**
 * User: Maurício Linhares
 * Date: 3/3/12
 * Time: 8:32 PM
 */

class QueryMessageEncoder ( charset : Charset ) extends Encoder {

  override def encode(message: FrontendMessage): ChannelBuffer = {

    val m = message.asInstanceOf[QueryMessage]

    val buffer = ChannelBuffers.dynamicBuffer()
    buffer.writeByte( Message.Query )
    buffer.writeInt(0)
    ChannelUtils.writeCString(m.query, buffer, charset)

    ChannelUtils.writeLength(  buffer )

    buffer
  }

}
