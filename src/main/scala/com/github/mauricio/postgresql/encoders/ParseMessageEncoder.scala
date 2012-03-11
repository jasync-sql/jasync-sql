package com.github.mauricio.postgresql.encoders

import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}
import com.github.mauricio.postgresql.messages.ParseMessage
import com.github.mauricio.postgresql.{ChannelUtils, FrontendMessage, CharsetHelper}
import com.github.mauricio.postgresql.util.Log


/**
 * User: Maurício Linhares
 * Date: 3/7/12
 * Time: 9:20 AM
 */

object ParseMessageEncoder extends Encoder {

  private val log = Log.getByName("ParseMessageEncoder")

  override def encode(message: FrontendMessage): ChannelBuffer = {

    val m = message.asInstanceOf[ParseMessage]

    val queryBytes = CharsetHelper.toBytes(m.command)
    val columnCount = m.parameterTypes.size

    val buffer = ChannelBuffers.dynamicBuffer( 1024 )

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

    ChannelUtils.writeLength(buffer)

    buffer
  }

}
