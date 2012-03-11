package com.github.mauricio.postgresql.encoders

import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}
import com.github.mauricio.postgresql.messages.StartupMessage
import com.github.mauricio.postgresql.util.Log
import com.github.mauricio.postgresql.{OutputBuffer, FrontendMessage, ChannelUtils}


/**
 * User: Maurício Linhares
 * Date: 3/3/12
 * Time: 7:35 PM
 */

object StartupMessageEncoder extends Encoder {

  private val log = Log.getByName("StartupMessageEncoder")

  override def encode(message: FrontendMessage): ChannelBuffer = {

    val startup = message.asInstanceOf[StartupMessage]

    val output = new OutputBuffer()

    output.writeInteger2(3)
    output.writeInteger2(0)

    startup.parameters.foreach {
      pair =>
        output.writeCString(pair._1)
        output.writeCString(pair._2)
    }

    output.writeByte(0)

    output.toBuffer
  }

}
