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

package com.github.mauricio.async.db.mysql.decoder

import com.github.mauricio.async.db.mysql.message.server.{HandshakeMessage, ServerMessage}
import com.github.mauricio.async.db.util.{Log, ChannelUtils}
import java.nio.charset.Charset
import org.jboss.netty.buffer.ChannelBuffer

object HandshakeV10Decoder {
  val log = Log.get[HandshakeV10Decoder]
}

class HandshakeV10Decoder( charset : Charset ) extends MessageDecoder {

  import HandshakeV10Decoder.log

  def decode(buffer: ChannelBuffer): ServerMessage = {

    val serverVersion = ChannelUtils.readCString(buffer, charset)
    val connectionId = buffer.readInt()
    var seed = buffer.readSlice(8).toString(charset)
    var serverCapabilityFlags : Int = buffer.readShort()

    if ( buffer.readableBytes() > 0 ) {
      val characterSet = buffer.readByte() & 0xff
      val statusFlags = buffer.readShort()

      serverCapabilityFlags += 65536 * buffer.readShort().asInstanceOf[Int]

      val authPluginDataLength = buffer.readByte() & 0xff
      var authenticationMethod : Option[String] = None

      if ( authPluginDataLength > 0 ) {
        buffer.readerIndex( buffer.readerIndex() + 16 )
        seed += ChannelUtils.readUntilEOF(buffer, charset)
        authenticationMethod = Some(ChannelUtils.readUntilEOF(buffer, charset))
      }

      new HandshakeMessage(
        serverVersion,
        connectionId,
        seed,
        serverCapabilityFlags,
        characterSet = Some(characterSet),
        statusFlags = Some(statusFlags),
        authenticationMethod = authenticationMethod
      )
    } else {
      new HandshakeMessage(
        serverVersion,
        connectionId,
        seed,
        serverCapabilityFlags )
    }

  }

}