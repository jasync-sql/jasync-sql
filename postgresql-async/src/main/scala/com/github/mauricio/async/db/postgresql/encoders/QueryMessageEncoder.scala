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

import com.github.mauricio.async.db.postgresql.messages.backend.ServerMessage
import com.github.mauricio.async.db.postgresql.messages.frontend.{QueryMessage, ClientMessage}
import com.github.mauricio.async.db.util.{Log, ByteBufferUtils}
import java.nio.charset.Charset
import io.netty.buffer.{Unpooled, ByteBuf}

object QueryMessageEncoder {
  val log = Log.get[QueryMessageEncoder]
}

class QueryMessageEncoder(charset: Charset) extends Encoder {

  import QueryMessageEncoder.log

  override def encode(message: ClientMessage): ByteBuf = {

    val m = message.asInstanceOf[QueryMessage]

    if ( log.isDebugEnabled ) {
      log.debug("Executing direct query ({})", m.query)
    }

    val buffer = Unpooled.buffer()
    buffer.writeByte(ServerMessage.Query)
    buffer.writeInt(0)
    ByteBufferUtils.writeCString(m.query, buffer, charset)

    ByteBufferUtils.writeLength(buffer)

    buffer
  }

}
