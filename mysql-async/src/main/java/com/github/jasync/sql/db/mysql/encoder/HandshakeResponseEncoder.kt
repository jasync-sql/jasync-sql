package com.github.jasync.sql.db.mysql.encoder

import com.github.jasync.sql.db.exceptions.UnsupportedAuthenticationMethodException
import com.github.jasync.sql.db.mysql.encoder.auth.AuthenticationMethod
import com.github.jasync.sql.db.mysql.message.client.ClientMessage
import com.github.jasync.sql.db.mysql.message.client.HandshakeResponseMessage
import com.github.jasync.sql.db.mysql.util.CharsetMapper
import com.github.jasync.sql.db.mysql.util.MySQLIO.CLIENT_CONNECT_ATTRS
import com.github.jasync.sql.db.mysql.util.MySQLIO.CLIENT_CONNECT_WITH_DB
import com.github.jasync.sql.db.mysql.util.MySQLIO.CLIENT_MULTI_RESULTS
import com.github.jasync.sql.db.mysql.util.MySQLIO.CLIENT_PLUGIN_AUTH
import com.github.jasync.sql.db.mysql.util.MySQLIO.CLIENT_PROTOCOL_41
import com.github.jasync.sql.db.mysql.util.MySQLIO.CLIENT_SECURE_CONNECTION
import com.github.jasync.sql.db.mysql.util.MySQLIO.CLIENT_TRANSACTIONS
import com.github.jasync.sql.db.util.ByteBufferUtils
import com.github.jasync.sql.db.util.length
import com.github.jasync.sql.db.util.writeLength
import com.github.jasync.sql.db.util.writeLengthEncodedString
import io.netty.buffer.ByteBuf
import java.nio.charset.Charset

class HandshakeResponseEncoder(val charset: Charset, val charsetMapper: CharsetMapper) : MessageEncoder {

    companion object {
        const val APP_NAME_KEY = "_client_name"
        const val MAX_3_BYTES = 0x00ffffff
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

        if (m.appName != null) {
            clientCapabilities = clientCapabilities or CLIENT_CONNECT_ATTRS
        }

        val buffer = ByteBufferUtils.packetBuffer()

        buffer.writeInt(clientCapabilities)
        buffer.writeInt(MAX_3_BYTES)
        buffer.writeByte(charsetMapper.toInt(charset))
        buffer.writeBytes(PADDING)
        ByteBufferUtils.writeCString(m.username, buffer, charset)

        if (m.password != null) {
            val method = m.authenticationMethod
            val authenticator = this.authenticationMethods.getOrElse(
                method
            ) { throw UnsupportedAuthenticationMethodException(method) }
            val bytes = authenticator.generateAuthentication(charset, m.password, m.seed)
            buffer.writeByte(bytes.length)
            buffer.writeBytes(bytes)
        } else {
            buffer.writeByte(0)
        }

        if (m.database != null) {
            ByteBufferUtils.writeCString(m.database, buffer, charset)
        }

        ByteBufferUtils.writeCString(m.authenticationMethod, buffer, charset)

        if (m.appName != null)
        {
            //CONNECTION_ATTRS <lenenc-int><lenenc-str-key><lenenc-str-value>
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
