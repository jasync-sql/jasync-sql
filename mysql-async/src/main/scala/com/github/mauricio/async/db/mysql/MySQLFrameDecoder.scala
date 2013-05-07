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

package com.github.mauricio.async.db.mysql

import com.github.mauricio.async.db.exceptions.{BufferNotFullyConsumedException, ParserNotAvailableException}
import com.github.mauricio.async.db.mysql.decoder.{ErrorDecoder, HandshakeV10Decoder}
import com.github.mauricio.async.db.mysql.message.server.ServerMessage
import com.github.mauricio.async.db.util.ChannelUtils.readLongInt
import com.github.mauricio.async.db.util.Log
import java.nio.charset.Charset
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.{Channel, ChannelHandlerContext}
import org.jboss.netty.handler.codec.frame.FrameDecoder

object MySQLFrameDecoder {
  val log = Log.get[MySQLFrameDecoder]
}

class MySQLFrameDecoder(charset: Charset) extends FrameDecoder {

  private val handshakeDecoder = new HandshakeV10Decoder(charset)
  private val errorDecoder = new ErrorDecoder(charset)

  def decode(ctx: ChannelHandlerContext, channel: Channel, buffer: ChannelBuffer): AnyRef = {
    if (buffer.readableBytes() > 4) {

      buffer.markReaderIndex()

      val size = readLongInt(buffer)
      val sequence = buffer.readByte()

      if (buffer.readableBytes() >= size) {
        val messageType = buffer.readByte()

        val slice = buffer.readSlice(size - 1)

        val decoder = messageType match {
          case ServerMessage.ServerProtocolVersion => this.handshakeDecoder
          case ServerMessage.Error => this.errorDecoder
          case _ => {
            throw new ParserNotAvailableException(messageType)
          }
        }

        val result = decoder.decode(slice)

        if ( slice.readableBytes() != 0 ) {
          throw new BufferNotFullyConsumedException(slice)
        }

        return result

      } else {
        buffer.resetReaderIndex()
      }

    }

    return null
  }

}
