package com.github.mauricio.postgresql.parsers

import org.specs2.mutable.Specification
import org.jboss.netty.buffer.ChannelBuffers
import com.github.mauricio.postgresql.Message

/**
 * User: Maurício Linhares
 * Date: 2/28/12
 * Time: 11:33 PM
 */

class ParserKSpec extends Specification {

  val parser = new ParserK()

  "parserk" should {

    "correctly parse the message" in {

      val buffer = ChannelBuffers.dynamicBuffer()
      buffer.writeInt(10)
      buffer.writeInt(20)

      val message = parser.parseMessage( buffer )
      val data = message.content.asInstanceOf[ProcessData]

      List(
        message.name === Message.BackendKeyData,
        data.processId === 10,
        data.secretKey === 20
      )
    }

  }

}
