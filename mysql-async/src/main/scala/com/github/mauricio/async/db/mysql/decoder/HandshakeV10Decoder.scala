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
  val SeedSize = 8
  val SeedComplementSize = 12
  val Padding = 10
}

class HandshakeV10Decoder(charset: Charset) extends MessageDecoder {

  import HandshakeV10Decoder._

  def decode(buffer: ChannelBuffer): ServerMessage = {

    val serverVersion = ChannelUtils.readCString(buffer, charset)
    val connectionId = buffer.readInt()

    var seed = new Array[Byte]( SeedSize + SeedComplementSize )
    buffer.readBytes(seed, 0, SeedSize)

    // filler
    buffer.readByte()

    var serverCapabilityFlags: Int = buffer.readShort()

    val characterSet = buffer.readByte() & 0xff
    val statusFlags = buffer.readShort()

    serverCapabilityFlags += 65536 * buffer.readShort().asInstanceOf[Int]

    val authPluginDataLength = buffer.readUnsignedByte()
    var authenticationMethod: Option[String] = None

    if (authPluginDataLength > 0) {
      log.debug("Auth plugin data size {}", authPluginDataLength)
      buffer.readerIndex(buffer.readerIndex() + Padding)
      buffer.readBytes(seed, SeedSize, SeedComplementSize)

      var count = 0
      while ( count < seed.length ) {
        if ( seed(count) == 0 ) {
          log.debug("Index {} is zeroed", count)
        }
        count += 1
      }

      buffer.readByte()
      authenticationMethod = Some(ChannelUtils.readUntilEOF(buffer, charset))
    }

    log.debug("Full seed is {}", new String( seed ))
    log.debug("Method is {}", authenticationMethod)

    new HandshakeMessage(
      serverVersion,
      connectionId,
      seed,
      serverCapabilityFlags,
      characterSet = Some(characterSet),
      statusFlags = Some(statusFlags),
      authenticationMethod = authenticationMethod
    )
  }

}