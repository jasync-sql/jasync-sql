package com.github.mauricio.postgresql

import messages.backend.ErrorMessage
import org.specs2.mutable.Specification
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil

/**
 * User: Maurício Linhares
 * Date: 2/28/12
 * Time: 10:33 PM
 */

class MessageDecoderSpec extends Specification {

  val decoder = MessageDecoder

  "message decoder" should {

    "not try to decode if there is not enought data available" in {

      val buffer = ChannelBuffers.dynamicBuffer()

      buffer.writeByte('R')
      buffer.writeByte(1)
      buffer.writeByte(2)

      this.decoder.decode( null, null, buffer ) must beNull
    }

    "should not try to decode if there is a type and lenght but it's not long enough" in {

      val buffer = ChannelBuffers.dynamicBuffer()

      buffer.writeByte('R')
      buffer.writeInt( 30 )
      buffer.writeBytes( "my-name".getBytes( CharsetUtil.UTF_8 ) )

      List(
        this.decoder.decode( null, null, buffer ) must beNull,
        buffer.readerIndex() === 0
      )
    }

    "should correctly decode a message" in {

      val buffer =  ChannelBuffers.dynamicBuffer()
      val text = "This is an error message"
      val textBytes = text.getBytes( CharsetUtil.UTF_8 )

      buffer.writeByte('E')
      buffer.writeInt( textBytes.length + 4 + 1 )
      buffer.writeByte('M')
      buffer.writeBytes( textBytes )

      val result = this.decoder.decode( null, null, buffer ).asInstanceOf[ErrorMessage]

      List(
        result.message === text,
        buffer.readerIndex() === (textBytes.length + 5 )
      )
    }

  }


}
