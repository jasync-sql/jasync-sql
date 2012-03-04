package com.github.mauricio.postgresql.encoders

import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}


/**
 * User: Maurício Linhares
 * Date: 3/3/12
 * Time: 8:41 PM
 */

object CloseMessageEncoder extends Encoder {

  def encode(message: AnyRef): ChannelBuffer = {
    val buffer = ChannelBuffers.buffer(5)
    buffer.writeByte('X')
    buffer.writeInt(0)

    buffer
  }

}
