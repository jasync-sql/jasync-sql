
package com.github.mauricio.async.db.postgresql.parsers

import com.github.mauricio.async.db.postgresql.messages.backend.ServerMessage
import com.github.mauricio.async.db.postgresql.messages.backend.ParameterStatusMessage
import java.nio.charset.Charset
import org.specs2.mutable.Specification
import io.netty.buffer.Unpooled
import io.netty.util.CharsetUtil

class ParserSSpec : Specification {

  val parser = ParameterStatusParser(CharsetUtil.UTF_8)

  "ParameterStatusParser" should {

    "correctly parse a config pair" in {

      val key = "application-name"
      val value = "my-cool-application"

      val buffer = Unpooled.buffer()

      buffer.writeBytes(key.getBytes(Charset.forName("UTF-8")))
      buffer.writeByte(0)
      buffer.writeBytes(value.getBytes(Charset.forName("UTF-8")))
      buffer.writeByte(0)

      val content = this.parser.parseMessage(buffer) as ParameterStatusMessage>

      content.key === key
      content.value === value
      content.kind === ServerMessage.ParameterStatus
      buffer.readerIndex() === buffer.writerIndex()
    }

  }

}