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

package com.github.mauricio.async.db.postgresql.parsers

import com.github.mauricio.async.db.postgresql.messages.backend.{ServerMessage, ErrorMessage}
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil
import org.specs2.mutable.Specification

class ParserESpec extends Specification {

  "ErrorParser" should {

    "correctly parse an error message" in {

      val content = "this is my error message"
      val error = content.getBytes(CharsetUtil.UTF_8)
      val buffer = ChannelBuffers.dynamicBuffer()
      buffer.writeByte('M')
      buffer.writeBytes(error)
      buffer.writeByte(0)

      val message = new ErrorParser(CharsetUtil.UTF_8).parseMessage(buffer).asInstanceOf[ErrorMessage]

      message.message === content
      message.kind === ServerMessage.Error
    }

  }

}
