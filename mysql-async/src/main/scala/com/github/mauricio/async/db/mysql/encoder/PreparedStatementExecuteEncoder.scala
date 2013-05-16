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

import com.github.mauricio.async.db.mysql.binary.BinaryRowEncoder
import com.github.mauricio.async.db.mysql.message.client.{PreparedStatementExecuteMessage, ClientMessage}
import com.github.mauricio.async.db.util.ChannelUtils
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}

class PreparedStatementExecuteEncoder( rowEncoder : BinaryRowEncoder ) extends MessageEncoder {

  def encode(message: ClientMessage): ChannelBuffer = {
    val m = message.asInstanceOf[PreparedStatementExecuteMessage]

    val buffer = ChannelUtils.packetBuffer()
    buffer.writeByte( m.kind )
    buffer.writeBytes(m.statementId)
    buffer.writeByte(0x00) // no cursor
    buffer.writeInt(1)

    if ( m.parameters.isEmpty ) {
      buffer
    } else {
      val parametersBuffer = rowEncoder.encode(m.values)
      ChannelBuffers.wrappedBuffer(buffer, parametersBuffer)
    }

  }

}