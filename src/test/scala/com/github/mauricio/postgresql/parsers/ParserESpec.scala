package com.github.mauricio.postgresql.parsers

import org.specs2.mutable.Specification
import org.jboss.netty.buffer.ChannelBuffers
import com.github.mauricio.postgresql.Message

/**
 * User: Maurício Linhares
 * Date: 2/28/12
 * Time: 9:54 PM
 */

class ParserESpec extends Specification {

  "ParserE" should {

    "correctly parse an error message" in {

      val error = "this is my error message"
      val buffer = ChannelBuffers.dynamicBuffer()
      buffer.writeBytes( error.getBytes )

      val message = ParserE.parseMessage( buffer )

      List(message.content === error, message.name === Message.Error)
    }

  }

}
