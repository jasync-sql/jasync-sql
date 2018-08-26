
package com.github.mauricio.async.db.mysql.encoder

import com.github.jasync.sql.db.exceptions.UnsupportedAuthenticationMethodException
import com.github.jasync.sql.db.util.ByteBufferUtils
import com.github.jasync.sql.db.util.length
import com.github.mauricio.async.db.mysql.encoder.auth.AuthenticationMethod
import com.github.mauricio.async.db.mysql.message.client.ClientMessage
import com.github.mauricio.async.db.mysql.message.client.HandshakeResponseMessage
import com.github.mauricio.async.db.mysql.util.CharsetMapper
import com.github.mauricio.async.db.mysql.util.MySQLIO.CLIENT_CONNECT_WITH_DB
import com.github.mauricio.async.db.mysql.util.MySQLIO.CLIENT_MULTI_RESULTS
import com.github.mauricio.async.db.mysql.util.MySQLIO.CLIENT_PLUGIN_AUTH
import com.github.mauricio.async.db.mysql.util.MySQLIO.CLIENT_PROTOCOL_41
import com.github.mauricio.async.db.mysql.util.MySQLIO.CLIENT_SECURE_CONNECTION
import com.github.mauricio.async.db.mysql.util.MySQLIO.CLIENT_TRANSACTIONS
import io.netty.buffer.ByteBuf
import mu.KotlinLogging
import java.nio.charset.Charset

private val logger = KotlinLogging.logger {}


class HandshakeResponseEncoder(val charset: Charset, val charsetMapper: CharsetMapper) : MessageEncoder {

  companion object {
  val MAX_3_BYTES = 0x00ffffff
  val PADDING: ByteArray = ByteArray(23) {
    0.toByte()
  }

  }

  private val authenticationMethods = AuthenticationMethod.Availables

  override fun encode(message: ClientMessage): ByteBuf {

    val m = message as HandshakeResponseMessage

    var clientCapabilities = 0

    clientCapabilities = clientCapabilities or
      CLIENT_PLUGIN_AUTH or
      CLIENT_PROTOCOL_41 or
      CLIENT_TRANSACTIONS or
      CLIENT_MULTI_RESULTS or
      CLIENT_SECURE_CONNECTION

    if (m.database != null) {
      clientCapabilities = clientCapabilities or CLIENT_CONNECT_WITH_DB
    }

    val buffer = ByteBufferUtils.packetBuffer()

    buffer.writeInt(clientCapabilities)
    buffer.writeInt(MAX_3_BYTES)
    buffer.writeByte(charsetMapper.toInt(charset))
    buffer.writeBytes(PADDING)
    ByteBufferUtils.writeCString( m.username, buffer, charset )

    if ( m.password != null ) {
      val method = m.authenticationMethod
      val authenticator = this.authenticationMethods.getOrElse(
        method) { throw UnsupportedAuthenticationMethodException(method) }
      val bytes = authenticator.generateAuthentication(charset, m.password, m.seed)
      buffer.writeByte(bytes.length)
      buffer.writeBytes(bytes)
    } else {
      buffer.writeByte(0)
    }

    if ( m.database != null ) {
      ByteBufferUtils.writeCString( m.database, buffer, charset )
    }

    ByteBufferUtils.writeCString( m.authenticationMethod, buffer, charset )

    return buffer
  }

}
