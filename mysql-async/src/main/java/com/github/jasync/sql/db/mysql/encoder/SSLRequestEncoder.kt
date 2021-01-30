package com.github.jasync.sql.db.mysql.encoder

import com.github.jasync.sql.db.mysql.message.client.ClientMessage
import com.github.jasync.sql.db.mysql.message.client.SSLRequestMessage
import com.github.jasync.sql.db.mysql.util.CharsetMapper
import com.github.jasync.sql.db.mysql.util.MySQLIO
import com.github.jasync.sql.db.mysql.util.MySQLIO.CLIENT_MULTI_RESULTS
import com.github.jasync.sql.db.mysql.util.MySQLIO.CLIENT_PLUGIN_AUTH
import com.github.jasync.sql.db.mysql.util.MySQLIO.CLIENT_PROTOCOL_41
import com.github.jasync.sql.db.mysql.util.MySQLIO.CLIENT_SECURE_CONNECTION
import com.github.jasync.sql.db.mysql.util.MySQLIO.CLIENT_SSL
import com.github.jasync.sql.db.mysql.util.MySQLIO.CLIENT_TRANSACTIONS
import com.github.jasync.sql.db.util.ByteBufferUtils
import io.netty.buffer.ByteBuf
import java.nio.charset.Charset

class SSLRequestEncoder(private val charset: Charset, private val charsetMapper: CharsetMapper) : MessageEncoder {

    companion object {
        private const val MAX_3_BYTES = 0x00ffffff
        private val PADDING: ByteArray = ByteArray(23) { 0.toByte() }
    }

    override fun encode(message: ClientMessage): ByteBuf {
        require(message is SSLRequestMessage)
        var clientCapabilities = 0

        clientCapabilities = clientCapabilities or
                CLIENT_PLUGIN_AUTH or
                CLIENT_PROTOCOL_41 or
                CLIENT_TRANSACTIONS or
                CLIENT_MULTI_RESULTS or
                CLIENT_SECURE_CONNECTION or
                CLIENT_SSL

        if (message.connectWithDb) {
            clientCapabilities = clientCapabilities or MySQLIO.CLIENT_CONNECT_WITH_DB
        }

        if (message.hasAppName) {
            clientCapabilities = clientCapabilities or MySQLIO.CLIENT_CONNECT_ATTRS
        }
        val buffer = ByteBufferUtils.packetBuffer()

        buffer.writeInt(clientCapabilities)
        buffer.writeInt(MAX_3_BYTES)
        buffer.writeByte(charsetMapper.toInt(charset))
        buffer.writeBytes(PADDING)

        return buffer
    }
}
