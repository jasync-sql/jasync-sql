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

import com.github.mauricio.async.db.postgresql.messages.backend.Message
import com.github.mauricio.async.db.postgresql.messages.frontend.{FrontendMessage, PreparedStatementExecuteMessage}
import java.nio.charset.Charset
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}
import com.github.mauricio.async.db.util.ChannelUtils
import com.github.mauricio.async.db.column.ColumnEncoderRegistry

class ExecutePreparedStatementEncoder(charset: Charset, encoder : ColumnEncoderRegistry) extends Encoder {

  def encode(message: FrontendMessage): ChannelBuffer = {

    val m = message.asInstanceOf[PreparedStatementExecuteMessage]

    val queryBytes = m.query.getBytes(charset)

    val bindBuffer = ChannelBuffers.dynamicBuffer(1024)

    bindBuffer.writeByte(Message.Bind)
    bindBuffer.writeInt(0)

    bindBuffer.writeBytes(queryBytes)
    bindBuffer.writeByte(0)
    bindBuffer.writeBytes(queryBytes)
    bindBuffer.writeByte(0)

    bindBuffer.writeShort(0)

    bindBuffer.writeShort(m.values.length)

    for (value <- m.values) {
      if (value == null) {
        bindBuffer.writeInt(-1)
      } else {
        val encoded = encoder.encode(value).getBytes(charset)
        bindBuffer.writeInt(encoded.length)
        bindBuffer.writeBytes(encoded)
      }
    }

    bindBuffer.writeShort(0)

    ChannelUtils.writeLength(bindBuffer)

    val executeLength = 1 + 4 + queryBytes.length + 1 + 4
    val executeBuffer = ChannelBuffers.buffer(executeLength)
    executeBuffer.writeByte(Message.Execute)
    executeBuffer.writeInt(executeLength - 1)

    executeBuffer.writeBytes(queryBytes)
    executeBuffer.writeByte(0)

    executeBuffer.writeInt(0)

    val closeLength = 1 + 4 + 1 + queryBytes.length + 1
    val closeBuffer = ChannelBuffers.buffer(closeLength)
    closeBuffer.writeByte(Message.CloseStatementOrPortal)
    closeBuffer.writeInt(closeLength - 1)
    closeBuffer.writeByte('P')

    closeBuffer.writeBytes(queryBytes)
    closeBuffer.writeByte(0)

    val syncBuffer = ChannelBuffers.buffer(5)
    syncBuffer.writeByte(Message.Sync)
    syncBuffer.writeInt(4)

    ChannelBuffers.wrappedBuffer(bindBuffer, executeBuffer, syncBuffer, closeBuffer)

  }
}
