package com.github.mauricio.postgresql.encoders

import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}
import com.github.mauricio.postgresql.messages.StartupMessage
import com.github.mauricio.postgresql.util.Log
import com.github.mauricio.postgresql.{FrontendMessage, ChannelUtils}


/**
 * User: Maurício Linhares
 * Date: 3/3/12
 * Time: 7:35 PM
 */

object StartupMessageEncoder extends Encoder {

  private val log = Log.getByName("StartupMessageEncoder")

  override def encode(message: FrontendMessage): ChannelBuffer = {

    val startup = message.asInstanceOf[StartupMessage]

    val buffer = ChannelBuffers.dynamicBuffer()
    buffer.writeInt(0)
    buffer.writeShort( 3 )
    buffer.writeShort( 0 )

    startup.parameters.foreach {
      pair =>
        ChannelUtils.writeCString( pair._1, buffer )
        ChannelUtils.writeCString( pair._2, buffer )
    }

    buffer.writeByte(0)

    val index = buffer.writerIndex()

    buffer.markWriterIndex()
    buffer.writerIndex(0)
    buffer.writeInt(index)
    buffer.resetWriterIndex()

    buffer
  }

}
