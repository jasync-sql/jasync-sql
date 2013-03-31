package com.github.mauricio.postgresql.parsers

import org.specs2.mutable.Specification
import org.jboss.netty.buffer.ChannelBuffers
import com.github.mauricio.postgresql.messages.backend.{ErrorMessage, Message}

/**
 * User: Maurício Linhares
 * Date: 2/28/12
 * Time: 9:54 PM
 */

class ParserESpec extends Specification {

  "ErrorParser" should {

    "correctly parse an error message" in {

      val error = "this is my error message"
      val buffer = ChannelBuffers.dynamicBuffer()
      buffer.writeByte('M')
      buffer.writeBytes( error.getBytes )

      val message = ErrorParser.parseMessage( buffer ).asInstanceOf[ErrorMessage]

      List(message.message === error, message.name === Message.Error)
    }

  }

}
