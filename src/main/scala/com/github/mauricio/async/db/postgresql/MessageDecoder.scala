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

package com.github.mauricio.postgresql

import com.github.mauricio.async.db.util.Log
import com.github.mauricio.postgresql.parsers.{MessageParser, AuthenticationStartupParser}
import java.nio.charset.Charset
import messages.backend.Message
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.{ChannelHandlerContext, Channel}
import org.jboss.netty.handler.codec.frame.FrameDecoder

object MessageDecoder {
  val log = Log.get[MessageDecoder]
}

class MessageDecoder ( charset : Charset ) extends FrameDecoder {

  private val parser = new MessageParser(charset)

  override def decode(ctx: ChannelHandlerContext, c: Channel, b: ChannelBuffer) : Object = {

    if ( b.readableBytes() >= 5 ) {

      b.markReaderIndex()

      val code = b.readByte().asInstanceOf[Char]
      val lengthWithSelf = b.readInt()
      val length = lengthWithSelf - 4

      if ( b.readableBytes() >= length ) {
        code match {
          case Message.Authentication => {
            AuthenticationStartupParser.parseMessage( b )
          }
          case _ => {
            parser.parse( code, b.readSlice( length ) )
          }
        }

      } else {
        b.resetReaderIndex()
        return null
      }

    } else {
      return null
    }

  }

}