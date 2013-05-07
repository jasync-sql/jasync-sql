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

import com.github.mauricio.async.db.mysql.message.client.{HandshakeResponseMessage, ClientMessage}
import org.jboss.netty.buffer.ChannelBuffer
import java.nio.charset.Charset
import com.github.mauricio.async.db.mysql.util.CharsetMapper
import com.github.mauricio.async.db.mysql.encoder.auth.MySQLNativePasswordAuthentication
import com.github.mauricio.async.db.util.ChannelUtils
import com.github.mauricio.async.db.exceptions.UnsupportedAuthenticationMethodException

object HandshakeResponseEncoder {

  final val CLIENT_COMPRESS = 32
  final val CLIENT_CONNECT_WITH_DB = 8
  final val CLIENT_FOUND_ROWS = 2
  final val CLIENT_LOCAL_FILES = 128
  /* Can use LOAD DATA LOCAL */
  final val CLIENT_LONG_FLAG = 4
  /* Get all column flags */
  final val CLIENT_LONG_PASSWORD = 1
  /* new more secure passwords */
  final val CLIENT_PROTOCOL_41 = 512
  // for > 4.1.1
  final val CLIENT_INTERACTIVE = 1024
  final val CLIENT_SSL = 2048
  final val CLIENT_TRANSACTIONS = 8192
  // Client knows about transactions
  final val CLIENT_RESERVED = 16384
  // for 4.1.0 only
  final val CLIENT_SECURE_CONNECTION = 32768
  final val CLIENT_MULTI_QUERIES = 65536
  // Enable/disable multiquery support
  final val CLIENT_MULTI_RESULTS = 131072
  // Enable/disable multi-results
  final val CLIENT_PLUGIN_AUTH = 524288
  final val CLIENT_CAN_HANDLE_EXPIRED_PASSWORD = 4194304
  final val CLIENT_CONNECT_ATTRS = 1048576
  final val MAX_3_BYTES = 255 * 255 * 255
  final val PADDING: Array[Byte] = List.fill(23) {
    0.toByte
  }.toArray
}

class HandshakeResponseEncoder(charset: Charset, charsetMapper: CharsetMapper) extends MessageEncoder {

  import HandshakeResponseEncoder._

  private val authenticationMethods = Map("mysql_native_password" -> new MySQLNativePasswordAuthentication(charset))

  def encode(message: ClientMessage): ChannelBuffer = {

    val m = message.asInstanceOf[HandshakeResponseMessage]

    var clientCapabilities = 0 |
      CLIENT_PLUGIN_AUTH |
      CLIENT_LONG_PASSWORD |
      CLIENT_PROTOCOL_41 |
      CLIENT_TRANSACTIONS |
      CLIENT_MULTI_RESULTS |
      CLIENT_SECURE_CONNECTION |
      CLIENT_LONG_FLAG

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
    } else {
      buffer.writeByte(0)
    }

    if ( m.authenticationMethod.isDefined ) {
      ChannelUtils.writeCString( m.authenticationMethod.get, buffer, charset )
    }

    buffer
  }

}
