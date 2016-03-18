/*
 * Copyright 2013 Maurício Linhares
 *
 * Maurício Linhares licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.github.mauricio.postgresql

import com.github.mauricio.async.db.postgresql.codec.MessageDecoder
import com.github.mauricio.async.db.postgresql.exceptions.{MessageTooLongException}
import com.github.mauricio.async.db.postgresql.messages.backend.{ServerMessage, ErrorMessage}
import org.specs2.mutable.Specification
import com.github.mauricio.async.db.exceptions.NegativeMessageSizeException
import io.netty.util.CharsetUtil
import io.netty.buffer.Unpooled
import java.util

class MessageDecoderSpec extends Specification {

  val decoder = new MessageDecoder(false, CharsetUtil.UTF_8)

  "message decoder" should {

    "not try to decode if there is not enought data available" in {

      val buffer = Unpooled.buffer()

      buffer.writeByte('R')
      buffer.writeByte(1)
      buffer.writeByte(2)
      val out = new util.ArrayList[Object]()

      this.decoder.decode(null, buffer, out)
      out.isEmpty
    }

    "should not try to decode if there is a type and lenght but it's not long enough" in {

      val buffer = Unpooled.buffer()

      buffer.writeByte('R')
      buffer.writeInt(30)
      buffer.writeBytes("my-name".getBytes(CharsetUtil.UTF_8))

      val out = new util.ArrayList[Object]()
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
      val out = new util.ArrayList[Object]()
      this.decoder.decode(null, buffer, out)
      out.size === 1
      val result = out.get(0).asInstanceOf[ErrorMessage]
      result.message === text
      buffer.readerIndex() === (textBytes.length + 4 + 1 + 1 + 1)
    }

    "should raise an exception if the length is negative" in {
      val buffer = Unpooled.buffer()
      buffer.writeByte( ServerMessage.Close )
      buffer.writeInt( 2 )
      val out = new util.ArrayList[Object]()

      this.decoder.decode(null, buffer, out) must throwA[NegativeMessageSizeException]
    }

    "should raise an exception if the length is too big" in {

      val buffer = Unpooled.buffer()
      buffer.writeByte( ServerMessage.Close )
      buffer.writeInt( MessageDecoder.DefaultMaximumSize + 10 )
      val out = new util.ArrayList[Object]()

      this.decoder.decode(null, buffer, out) must throwA[MessageTooLongException]
    }

  }


}
