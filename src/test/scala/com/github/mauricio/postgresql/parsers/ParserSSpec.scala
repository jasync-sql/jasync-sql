package com.github.mauricio.postgresql.parsers

import org.specs2.mutable.Specification
import org.jboss.netty.buffer.ChannelBuffers
import java.nio.charset.Charset
import com.github.mauricio.postgresql.messages.backend.{ParameterStatusMessage, Message}
import org.jboss.netty.util.CharsetUtil

/**
 * User: Maurício Linhares
 * Date: 2/28/12
 * Time: 9:59 PM
 */

class ParserSSpec extends Specification {

  val parser = new ParameterStatusParser(CharsetUtil.UTF_8)

  "ParameterStatusParser" should {

    "correctly parse a config pair" in {

      val key  = "application-name"
      val value = "my-cool-application"

      val buffer = ChannelBuffers.dynamicBuffer()

      buffer.writeBytes( key.getBytes( Charset.forName("UTF-8") ) )
      buffer.writeByte(0)
      buffer.writeBytes( value.getBytes( Charset.forName("UTF-8") ) )
      buffer.writeByte(0)

      val content = this.parser.parseMessage( buffer ).asInstanceOf[ParameterStatusMessage]

      List(
        content.key === key,
        content.value === value,
        content.name === Message.ParameterStatus,
        buffer.readerIndex() === buffer.writerIndex() )
    }

  }

}
