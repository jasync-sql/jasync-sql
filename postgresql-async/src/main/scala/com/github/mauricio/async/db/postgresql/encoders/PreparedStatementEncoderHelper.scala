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

import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}
import com.github.mauricio.async.db.postgresql.messages.backend.ServerMessage
import com.github.mauricio.async.db.util.{Log, ChannelUtils}
import com.github.mauricio.async.db.column.ColumnEncoderRegistry
import java.nio.charset.Charset

object PreparedStatementEncoderHelper {
  final val log = Log.get[PreparedStatementEncoderHelper]
}

trait PreparedStatementEncoderHelper {

  import PreparedStatementEncoderHelper.log

  def writeExecutePortal(
                          statementIdBytes: Array[Byte],
                          values: Seq[Any],
                          encoder: ColumnEncoderRegistry,
                          charset: Charset,
                          writeDescribe: Boolean = false
                          ): ChannelBuffer = {

    val bindBuffer = ChannelBuffers.dynamicBuffer(1024)

    bindBuffer.writeByte(ServerMessage.Bind)
    bindBuffer.writeInt(0)

    bindBuffer.writeBytes(statementIdBytes)
    bindBuffer.writeByte(0)
    bindBuffer.writeBytes(statementIdBytes)
    bindBuffer.writeByte(0)

    bindBuffer.writeShort(0)

    bindBuffer.writeShort(values.length)

    for (value <- values) {
      if (value == null || value == None) {
        bindBuffer.writeInt(-1)
      } else {
        val content = encoder.encode(value).getBytes(charset)
        bindBuffer.writeInt(content.length)
        bindBuffer.writeBytes( content )
      }
    }

    bindBuffer.writeShort(0)

    ChannelUtils.writeLength(bindBuffer)

    if ( writeDescribe ) {
      val describeLength = 1 + 4 + 1 + statementIdBytes.length + 1
      val describeBuffer = bindBuffer
      describeBuffer.writeByte(ServerMessage.Describe)
      describeBuffer.writeInt(describeLength - 1)
      describeBuffer.writeByte('P')
      describeBuffer.writeBytes(statementIdBytes)
      describeBuffer.writeByte(0)
    }

    val executeLength = 1 + 4 + statementIdBytes.length + 1 + 4
    val executeBuffer = ChannelBuffers.buffer(executeLength)
    executeBuffer.writeByte(ServerMessage.Execute)
    executeBuffer.writeInt(executeLength - 1)
    executeBuffer.writeBytes(statementIdBytes)
    executeBuffer.writeByte(0)
    executeBuffer.writeInt(0)

    val closeLength = 1 + 4 + 1 + statementIdBytes.length + 1
    val closeBuffer = ChannelBuffers.buffer(closeLength)
    closeBuffer.writeByte(ServerMessage.CloseStatementOrPortal)
    closeBuffer.writeInt(closeLength - 1)
    closeBuffer.writeByte('P')
    closeBuffer.writeBytes(statementIdBytes)
    closeBuffer.writeByte(0)

    val syncBuffer = ChannelBuffers.buffer(5)
    syncBuffer.writeByte(ServerMessage.Sync)
    syncBuffer.writeInt(4)

    ChannelBuffers.wrappedBuffer(bindBuffer, executeBuffer, syncBuffer, closeBuffer)

  }

}
