
package com.github.mauricio.async.db.postgresql.parsers

import com.github.mauricio.async.db.postgresql.messages.backend.ServerMessage
import com.github.mauricio.async.db.postgresql.messages.backend.ProcessData
import org.specs2.mutable.Specification
import io.netty.buffer.Unpooled

class ParserKSpec : Specification {

  val parser = BackendKeyDataParser

  "parserk" should {

    "correctly parse the message" in {

      val buffer = Unpooled.buffer()
      buffer.writeInt(10)
      buffer.writeInt(20)

      val data = parser.parseMessage(buffer) as ProcessData>

      data.kind === ServerMessage.BackendKeyData
      data.processId === 10
      data.secretKey === 20

    }

  }

}