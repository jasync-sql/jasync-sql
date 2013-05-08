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

package com.github.mauricio.async.db.mysql.encoder

import com.github.mauricio.async.db.mysql.message.client.{QueryMessage, ClientMessage}
import org.jboss.netty.buffer.ChannelBuffer
import com.github.mauricio.async.db.util.ChannelUtils
import java.nio.charset.Charset

class QueryMessageEncoder( charset : Charset ) extends MessageEncoder {

  def encode(message: ClientMessage): ChannelBuffer = {

    val m = message.asInstanceOf[QueryMessage]
    val encodedQuery = m.query.getBytes( charset )
    val buffer = ChannelUtils.packetBuffer(4 + 1 + encodedQuery.length )
    buffer.writeByte( ClientMessage.Query )
    buffer.writeBytes( encodedQuery )

    buffer
  }

}