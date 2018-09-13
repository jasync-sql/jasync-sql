
package com.github.mauricio.postgresql

import com.github.mauricio.async.db.postgresql.codec.MessageDecoder
import com.github.mauricio.async.db.postgresql.exceptions.MessageTooLongException
import com.github.mauricio.async.db.postgresql.messages.backend.ServerMessage
import com.github.mauricio.async.db.postgresql.messages.backend.ErrorMessage
import org.specs2.mutable.Specification
import com.github.mauricio.async.db.exceptions.NegativeMessageSizeException
import io.netty.util.CharsetUtil
import io.netty.buffer.Unpooled
import java.util

class MessageDecoderSpec : Specification {

  val decoder = MessageDecoder(false, CharsetUtil.UTF_8)

  "message decoder" should {

    "not try to decode if there is not enought data available" in {

      val buffer = Unpooled.buffer()

      buffer.writeByte('R')
      buffer.writeByte(1)
      buffer.writeByte(2)
      val out = util.ArrayList<Object>()

      this.decoder.decode(null, buffer, out)
      out.isEmpty
    }

    "should not try to decode if there is a type and lenght but it's not long enough" in {

      val buffer = Unpooled.buffer()

      buffer.writeByte('R')
      buffer.writeInt(30)
      buffer.writeBytes("my-name".getBytes(CharsetUtil.UTF_8))

      val out = util.ArrayList<Object>()
      this.decoder.decode(null, buffer, out)
      buffer.readerIndex() === 0
    }

    "should correctly decode a message" in {

      val buffer = Unpooled.buffer()
      val text = "This is an error message"
      val textBytes = text.getBytes(CharsetUtil.UTF_8)

      buffer.writeByte('E')
      buffer.writeInt(textBytes.length + 4 + 1 + 1)
      buffer.writeByte('M')
      buffer.writeBytes(textBytes)
      buffer.writeByte(0)
      val out = util.ArrayList<Object>()
      this.decoder.decode(null, buffer, out)
      out.size === 1
      val result = out.get(0) as ErrorMessage>
      result.message === text
      buffer.readerIndex() === (textBytes.length + 4 + 1 + 1 + 1)
    }

    "should raise an exception if the length is negative" in {
      val buffer = Unpooled.buffer()
      buffer.writeByte( ServerMessage.Close )
      buffer.writeInt( 2 )
      val out = util.ArrayList<Object>()

      this.decoder.decode(null, buffer, out) must throwA<NegativeMessageSizeException>
    }

    "should raise an exception if the length is too big" in {

      val buffer = Unpooled.buffer()
      buffer.writeByte( ServerMessage.Close )
      buffer.writeInt( MessageDecoder.DefaultMaximumSize + 10 )
      val out = util.ArrayList<Object>()

      this.decoder.decode(null, buffer, out) must throwA<MessageTooLongException>
    }

  }


}