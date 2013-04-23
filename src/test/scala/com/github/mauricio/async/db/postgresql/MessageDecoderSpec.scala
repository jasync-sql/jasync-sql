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

import com.github.mauricio.async.db.postgresql.MessageDecoder
import com.github.mauricio.async.db.postgresql.messages.backend.ErrorMessage
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil
import org.specs2.mutable.Specification

class MessageDecoderSpec extends Specification {

  val decoder = new MessageDecoder(CharsetUtil.UTF_8)

  "message decoder" should {

    "not try to decode if there is not enought data available" in {

      val buffer = ChannelBuffers.dynamicBuffer()

      buffer.writeByte('R')
      buffer.writeByte(1)
      buffer.writeByte(2)

      this.decoder.decode(null, null, buffer) must beNull
    }

    "should not try to decode if there is a type and lenght but it's not long enough" in {

      val buffer = ChannelBuffers.dynamicBuffer()

      buffer.writeByte('R')
      buffer.writeInt(30)
      buffer.writeBytes("my-name".getBytes(CharsetUtil.UTF_8))

      List(
        this.decoder.decode(null, null, buffer) must beNull,
        buffer.readerIndex() === 0
      )
    }

    "should correctly decode a message" in {

      val buffer = ChannelBuffers.dynamicBuffer()
      val text = "This is an error message"
      val textBytes = text.getBytes(CharsetUtil.UTF_8)

      buffer.writeByte('E')
      buffer.writeInt(textBytes.length + 4 + 1 + 1)
      buffer.writeByte('M')
      buffer.writeBytes(textBytes)
      buffer.writeByte(0)

      val result = this.decoder.decode(null, null, buffer).asInstanceOf[ErrorMessage]

      result.message === text
      buffer.readerIndex() === (textBytes.length + 4 + 1 + 1 + 1)

    }

  }


}
