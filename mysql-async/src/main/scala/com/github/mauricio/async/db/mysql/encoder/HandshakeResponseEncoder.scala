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

import io.netty.buffer.ByteBuf
import com.github.mauricio.async.db.exceptions.UnsupportedAuthenticationMethodException
import com.github.mauricio.async.db.mysql.encoder.auth.MySQLNativePasswordAuthentication
import com.github.mauricio.async.db.mysql.message.client.{HandshakeResponseMessage, ClientMessage}
import com.github.mauricio.async.db.mysql.util.CharsetMapper
import com.github.mauricio.async.db.util.{Log, ChannelUtils}
import java.nio.charset.Charset

object HandshakeResponseEncoder {

  final val CLIENT_PROTOCOL_41 = 0x0200
  final val CLIENT_SECURE_CONNECTION = 0x8000
  final val CLIENT_CONNECT_WITH_DB = 0x0008
  final val CLIENT_TRANSACTIONS = 0x2000
  final val CLIENT_MULTI_RESULTS = 0x200000
  final val CLIENT_LONG_FLAG = 0x0001
  final val CLIENT_PLUGIN_AUTH = 524288

  final val MAX_3_BYTES = 0x00ffffff
  final val PADDING: Array[Byte] = List.fill(23) {
    0.toByte
  }.toArray

  final val log = Log.get[HandshakeResponseEncoder]

}

class HandshakeResponseEncoder(charset: Charset, charsetMapper: CharsetMapper) extends MessageEncoder {

  import HandshakeResponseEncoder._

  private val authenticationMethods = Map("mysql_native_password" -> new MySQLNativePasswordAuthentication(charset))

  def encode(message: ClientMessage): ByteBuf = {

    val m = message.asInstanceOf[HandshakeResponseMessage]

    var clientCapabilities = 0

    clientCapabilities |=
      CLIENT_PLUGIN_AUTH |
      CLIENT_PROTOCOL_41 |
      CLIENT_TRANSACTIONS |
      CLIENT_MULTI_RESULTS |
      CLIENT_SECURE_CONNECTION

    if (m.database.isDefined) {
      clientCapabilities |= CLIENT_CONNECT_WITH_DB
    }

    val buffer = ChannelUtils.packetBuffer()

    buffer.writeInt(clientCapabilities)
    buffer.writeInt(MAX_3_BYTES)
    buffer.writeByte(charsetMapper.toInt(charset))
    buffer.writeBytes(PADDING)
    ChannelUtils.writeCString( m.username, buffer, charset )

    if ( m.password.isDefined ) {
      val method = m.authenticationMethod.get
      val authenticator = this.authenticationMethods.getOrElse(
        method, { throw new UnsupportedAuthenticationMethodException(method) })
      val bytes = authenticator.generateAuthentication( m.username, m.password, m.seed )
      buffer.writeByte(bytes.length)
      buffer.writeBytes(bytes)
    } else {
      buffer.writeByte(0)
    }

    if ( m.database.isDefined ) {
      ChannelUtils.writeCString( m.database.get, buffer, charset )
    }

    if ( m.authenticationMethod.isDefined ) {
      ChannelUtils.writeCString( m.authenticationMethod.get, buffer, charset )
    } else {
      buffer.writeByte(0)
    }

    buffer

  }

}
