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

package com.github.mauricio.async.db.mysql.codec

import com.github.mauricio.async.db.mysql.message.server._
import com.github.mauricio.async.db.util.ChannelUtils
import com.github.mauricio.async.db.util.ChannelWrapper.bufferToWrapper
import org.jboss.netty.handler.codec.embedder.DecoderEmbedder
import org.jboss.netty.util.CharsetUtil
import org.specs2.mutable.Specification
import com.github.mauricio.async.db.mysql.message.server.OkMessage

class MySQLMessageDecoderSpec extends Specification {

  final val charset = CharsetUtil.UTF_8

  "decoder" should {

    "decode an OK message correctly" in {

      /*
1              [00] the OK header
lenenc-int     affected rows
lenenc-int     last-insert-id
  if capabilities & CLIENT_PROTOCOL_41 {
2              status_flags
2              warnings
  } elseif capabilities & CLIENT_TRANSACTIONS {
2              status_flags
  }
string[EOF]    info
       */


      val buffer = ChannelUtils.packetBuffer()
      buffer.writeByte(0)
      buffer.writeLength(10)
      buffer.writeLength(15)
      buffer.writeShort(5)
      buffer.writeShort(6)
      buffer.writeBytes( "this is a test".getBytes(charset) )
      buffer.writePacketLength()

      val decoder = this.createPipeline()

      decoder.offer(buffer)

      val ok = decoder.peek().asInstanceOf[OkMessage]
      ok.affectedRows === 10
      ok.lastInsertId === 15
      ok.message === "this is a test"
      ok.statusFlags === 5
      ok.warnings === 6
    }

    "decode an error message" in {

      /*
1              [ff] the ERR header
2              error code
  if capabilities & CLIENT_PROTOCOL_41 {
string[1]      '#' the sql-state marker
string[5]      sql-state
  }
string[EOF]    error-message
       */

      val content = "this is the error message"

      val buffer = ChannelUtils.packetBuffer()
      buffer.writeByte(0xff)
      buffer.writeShort(27)
      buffer.writeByte('H')
      buffer.writeBytes( "ZAWAY".getBytes(charset) )
      buffer.writeBytes(content.getBytes(charset))
      buffer.writePacketLength()

      val decoder = createPipeline()

      decoder.offer(buffer)

      val error = decoder.peek().asInstanceOf[ErrorMessage]

      error.errorCode === 27
      error.errorMessage === content
      error.sqlState === "HZAWAY"


    }

  }

  def createPipeline() : DecoderEmbedder[ServerMessage] = {
    new DecoderEmbedder[ServerMessage](new MySQLFrameDecoder(charset))
  }

}
