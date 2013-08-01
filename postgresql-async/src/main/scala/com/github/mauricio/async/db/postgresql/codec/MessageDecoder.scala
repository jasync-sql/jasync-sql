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

package com.github.mauricio.async.db.postgresql.codec

import com.github.mauricio.async.db.postgresql.exceptions.{MessageTooLongException}
import com.github.mauricio.async.db.postgresql.messages.backend.ServerMessage
import com.github.mauricio.async.db.postgresql.parsers.{AuthenticationStartupParser, MessageParsersRegistry}
import com.github.mauricio.async.db.util.Log
import java.nio.charset.Charset
import com.github.mauricio.async.db.exceptions.NegativeMessageSizeException
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.channel.ChannelHandlerContext
import io.netty.buffer.ByteBuf

object MessageDecoder {
  val log = Log.get[MessageDecoder]
  val DefaultMaximumSize = 16777216
}

class MessageDecoder(charset: Charset, maximumMessageSize : Int = MessageDecoder.DefaultMaximumSize) extends ByteToMessageDecoder {

  private val parser = new MessageParsersRegistry(charset)

  override def decode(ctx: ChannelHandlerContext,  b: ByteBuf, out: java.util.List[Object]): Unit = {

    if (b.readableBytes() >= 5) {

      b.markReaderIndex()

      val code = b.readByte()
      val lengthWithSelf = b.readInt()
      val length = lengthWithSelf - 4

      if ( length < 0 ) {
        throw new NegativeMessageSizeException(code, length)
      }

      if ( length > maximumMessageSize ) {
        throw new MessageTooLongException(code, length, maximumMessageSize)
      }

      if (b.readableBytes() >= length) {
        code match {
          case ServerMessage.Authentication => {
            val msg = AuthenticationStartupParser.parseMessage(b)
            out.add(msg)
          }
          case _ => {
            val msg = parser.parse(code, b.readSlice(length))
            out.add(msg)
          }
        }

      } else {
        b.resetReaderIndex()
        return
      }

    }

  }

}
