package com.github.jasync.sql.db.mysql.encoder

import com.github.jasync.sql.db.exceptions.UnsupportedAuthenticationMethodException
import com.github.jasync.sql.db.mysql.encoder.auth.AuthenticationMethod
import com.github.jasync.sql.db.mysql.message.client.ClientMessage
import com.github.jasync.sql.db.mysql.message.client.HandshakeResponseMessage
import com.github.jasync.sql.db.util.ByteBufferUtils
import com.github.jasync.sql.db.util.length
import com.github.jasync.sql.db.util.writeLength
import com.github.jasync.sql.db.util.writeLengthEncodedString
import io.netty.buffer.ByteBuf
import java.nio.charset.Charset

class HandshakeResponseEncoder(private val charset: Charset, private val headerEncoder: SSLRequestEncoder) : MessageEncoder {

    companion object {
        private const val APP_NAME_KEY = "_client_name"
    }

    private val authenticationMethods = AuthenticationMethod.Availables

    override fun encode(message: ClientMessage): ByteBuf {
        val m = message as HandshakeResponseMessage

        val buffer = headerEncoder.encode(m.header)

        ByteBufferUtils.writeCString(m.username, buffer, charset)

        if (m.password != null) {
            val method = m.authenticationMethod
            val authenticator = this.authenticationMethods.getOrElse(
                method
            ) { throw UnsupportedAuthenticationMethodException(method) }
            val bytes = authenticator.generateAuthentication(charset, m.configuration, m.seed)
            buffer.writeByte(bytes.length)
            buffer.writeBytes(bytes)
        } else {
            buffer.writeByte(0)
        }

        if (m.database != null) {
            ByteBufferUtils.writeCString(m.database, buffer, charset)
        }

        ByteBufferUtils.writeCString(m.authenticationMethod, buffer, charset)

        if (m.appName != null) {
            // CONNECTION_ATTRS <lenenc-int><lenenc-str-key><lenenc-str-value>
            val byteWidthEvaluator = {
                value: Int ->
                when {
                    value < 251 -> 1
                    value < (1 shl 16) -> 3
                    value < (1 shl 24) -> 4
                    else -> 9
                }
            }
            val key = APP_NAME_KEY.toByteArray(charset)
            val value = m.appName.toByteArray(charset)
            val totalLength = key.length + byteWidthEvaluator(key.length) + value.length + byteWidthEvaluator(value.length)
            buffer.writeLength(totalLength.toLong())
            buffer.writeLengthEncodedString(APP_NAME_KEY, charset)
            buffer.writeLengthEncodedString(m.appName, charset)
        }

        return buffer
    }
}
