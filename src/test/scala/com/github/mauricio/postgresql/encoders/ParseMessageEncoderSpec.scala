package com.github.mauricio.postgresql.encoders

import org.specs2.mutable.Specification
import com.github.mauricio.postgresql.messages.ParseMessage
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}
import com.github.mauricio.postgresql.CharsetHelper

/**
 * User: Maurício Linhares
 * Date: 3/10/12
 * Time: 11:57 PM
 */

class ParseMessageEncoderSpec extends Specification {

  "encoder" should {

    "generate a correct message" in {

      val query = "select 0"
      val message = new ParseMessage( query )

      val buffer = ChannelBuffers.dynamicBuffer()
      buffer.writeByte('P')
      buffer.writeInt( 4 + 9 + 9 + 2 )

      buffer.writeBytes( CharsetHelper.toBytes(query) )
      buffer.writeByte(0)
      buffer.writeBytes( CharsetHelper.toBytes(query) )
      buffer.writeByte(0)

      buffer.writeShort(0)

      buffer === ParseMessageEncoder.encode(message)
    }

  }

}
