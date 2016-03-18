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

package com.github.mauricio.async.db.postgresql.encoders

import com.github.mauricio.async.db.postgresql.messages.frontend.{ClientMessage, StartupMessage}
import com.github.mauricio.async.db.util.ByteBufferUtils
import java.nio.charset.Charset
import io.netty.buffer.{Unpooled, ByteBuf}

class StartupMessageEncoder(charset: Charset) {

  //private val log = Log.getByName("StartupMessageEncoder")

  def encode(startup: StartupMessage): ByteBuf = {

    val buffer = Unpooled.buffer()
    buffer.writeInt(0)
    buffer.writeShort(3)
    buffer.writeShort(0)

    startup.parameters.foreach {
      pair =>
        pair._2 match {
          case value: String => {
            ByteBufferUtils.writeCString(pair._1, buffer, charset)
            ByteBufferUtils.writeCString(value, buffer, charset)
          }
          case Some(value) => {
            ByteBufferUtils.writeCString(pair._1, buffer, charset)
            ByteBufferUtils.writeCString(value.toString, buffer, charset)
          }
          case _ => {}
        }
    }

    buffer.writeByte(0)

    val index = buffer.writerIndex()

    buffer.markWriterIndex()
    buffer.writerIndex(0)
    buffer.writeInt(index)
    buffer.resetWriterIndex()

    buffer
  }

}
