package com.github.mauricio.postgresql.encoders

import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}
import com.github.mauricio.postgresql.messages.StartupMessage
import com.github.mauricio.postgresql.{FrontendMessage, ChannelUtils}


/**
 * User: Maurício Linhares
 * Date: 3/3/12
 * Time: 7:35 PM
 */

object StartupMessageEncoder extends Encoder {

  override def encode(message: FrontendMessage): ChannelBuffer = {

    val m = message.asInstanceOf[StartupMessage]

    val buffer = ChannelBuffers.dynamicBuffer()

    buffer.writeInt(0)
    buffer.writeShort(3)
    buffer.writeShort(0)

    m.parameters.foreach {
      entry =>
        ChannelUtils.writeCString( entry._1, buffer )
        ChannelUtils.writeCString( entry._2, buffer )
    }

    buffer.writeByte(0)

    buffer.markWriterIndex()
    val length = buffer.writerIndex()

    buffer.writerIndex(0)
    buffer.writeInt(length)
    buffer.resetWriterIndex()

    buffer
  }

}
