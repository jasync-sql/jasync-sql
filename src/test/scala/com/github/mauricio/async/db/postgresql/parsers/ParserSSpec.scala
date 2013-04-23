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

package com.github.mauricio.postgresql.parsers

import com.github.mauricio.async.db.postgresql.messages.backend.{Message, ParameterStatusMessage}
import com.github.mauricio.async.db.postgresql.parsers.ParameterStatusParser
import java.nio.charset.Charset
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil
import org.specs2.mutable.Specification

class ParserSSpec extends Specification {

  val parser = new ParameterStatusParser(CharsetUtil.UTF_8)

  "ParameterStatusParser" should {

    "correctly parse a config pair" in {

      val key = "application-name"
      val value = "my-cool-application"

      val buffer = ChannelBuffers.dynamicBuffer()

      buffer.writeBytes(key.getBytes(Charset.forName("UTF-8")))
      buffer.writeByte(0)
      buffer.writeBytes(value.getBytes(Charset.forName("UTF-8")))
      buffer.writeByte(0)

      val content = this.parser.parseMessage(buffer).asInstanceOf[ParameterStatusMessage]

      List(
        content.key === key,
        content.value === value,
        content.name === Message.ParameterStatus,
        buffer.readerIndex() === buffer.writerIndex())
    }

  }

}
