package com.github.jasync.sql.db.mysql.encoder

import com.github.jasync.sql.db.mysql.message.client.CapabilityRequestMessage
import com.github.jasync.sql.db.mysql.message.client.ClientMessage
import com.github.jasync.sql.db.mysql.util.CharsetMapper
import com.github.jasync.sql.db.util.ByteBufferUtils
import io.netty.buffer.ByteBuf
import java.nio.charset.Charset

class SSLRequestEncoder(private val charset: Charset, private val charsetMapper: CharsetMapper) : MessageEncoder {

    companion object {
        private const val MAX_3_BYTES = 0x00ffffff
        private val PADDING: ByteArray = ByteArray(23) { 0.toByte() }
    }

    override fun encode(message: ClientMessage): ByteBuf {
        require(message is CapabilityRequestMessage)
        var clientCapabilities = 0

        for (flag in message.flags) {
            clientCapabilities = clientCapabilities or flag.value
        }

        val buffer = ByteBufferUtils.packetBuffer()

        buffer.writeInt(clientCapabilities)
        buffer.writeInt(MAX_3_BYTES)
        buffer.writeByte(charsetMapper.toInt(charset))
        buffer.writeBytes(PADDING)

        return buffer
    }
}
