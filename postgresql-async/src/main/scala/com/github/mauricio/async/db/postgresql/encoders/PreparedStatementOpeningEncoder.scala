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

import com.github.mauricio.async.db.column.ColumnEncoderRegistry
import com.github.mauricio.async.db.postgresql.messages.backend.ServerMessage
import com.github.mauricio.async.db.postgresql.messages.frontend.{ClientMessage, PreparedStatementOpeningMessage}
import com.github.mauricio.async.db.util.ChannelUtils
import java.nio.charset.Charset
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}

class PreparedStatementOpeningEncoder(charset: Charset, encoder : ColumnEncoderRegistry) extends Encoder {

  override def encode(message: ClientMessage): ChannelBuffer = {

    val m = message.asInstanceOf[PreparedStatementOpeningMessage]

    val emptyStringBytes = "".getBytes(charset)
    val queryBytes = m.query.getBytes(charset)
    val queryIdBytes = m.queryId.getBytes(charset)
    val columnCount = m.valueTypes.size

    val parseBuffer = ChannelBuffers.dynamicBuffer(1024)

    parseBuffer.writeByte(ServerMessage.Parse)
    parseBuffer.writeInt(0)

    parseBuffer.writeBytes(queryIdBytes)
    parseBuffer.writeByte(0)
    parseBuffer.writeBytes(queryBytes)
    parseBuffer.writeByte(0)

    parseBuffer.writeShort(columnCount)

    for (kind <- m.valueTypes) {
      parseBuffer.writeInt(kind)
    }

    ChannelUtils.writeLength(parseBuffer)

    val bindBuffer = ChannelBuffers.dynamicBuffer(1024)

    bindBuffer.writeByte(ServerMessage.Bind)
    bindBuffer.writeInt(0)

    bindBuffer.writeBytes(emptyStringBytes)
    bindBuffer.writeByte(0)
    bindBuffer.writeBytes(queryIdBytes)
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

    val describeLength = 1 + 4 + 1 + emptyStringBytes.length + 1
    val describeBuffer = ChannelBuffers.buffer(describeLength)
    describeBuffer.writeByte(ServerMessage.Describe)
    describeBuffer.writeInt(describeLength - 1)

    describeBuffer.writeByte('P')

    describeBuffer.writeBytes(emptyStringBytes)
    describeBuffer.writeByte(0)

    val executeLength = 1 + 4 + emptyStringBytes.length + 1 + 4
    val executeBuffer = ChannelBuffers.buffer(executeLength)
    executeBuffer.writeByte(ServerMessage.Execute)
    executeBuffer.writeInt(executeLength - 1)

    executeBuffer.writeBytes(emptyStringBytes)
    executeBuffer.writeByte(0)

    executeBuffer.writeInt(0)

    val closeLength = 1 + 4 + 1 + emptyStringBytes.length + 1
    val closeBuffer = ChannelBuffers.buffer(closeLength)
    closeBuffer.writeByte(ServerMessage.CloseStatementOrPortal)
    closeBuffer.writeInt(closeLength - 1)
    closeBuffer.writeByte('P')

    closeBuffer.writeBytes(emptyStringBytes)
    closeBuffer.writeByte(0)

    val syncBuffer = ChannelBuffers.buffer(5)
    syncBuffer.writeByte(ServerMessage.Sync)
    syncBuffer.writeInt(4)

    ChannelBuffers.wrappedBuffer(parseBuffer, bindBuffer, describeBuffer, executeBuffer, closeBuffer, syncBuffer)

  }

}
