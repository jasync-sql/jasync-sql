package com.github.jasync.sql.db.mysql.encoder

import com.github.jasync.sql.db.mysql.message.client.ClientMessage
import com.github.jasync.sql.db.mysql.message.client.QueryMessage
import com.github.jasync.sql.db.util.ByteBufferUtils
import io.netty.buffer.ByteBuf
import java.nio.charset.Charset

class QueryMessageEncoder(val charset: Charset) : MessageEncoder {

    override fun encode(message: ClientMessage): ByteBuf {
        val m = message as QueryMessage
        val encodedQuery = m.query.toByteArray(charset)
        val buffer = ByteBufferUtils.packetBuffer(4 + 1 + encodedQuery.size)
        buffer.writeByte(ClientMessage.Query)
        buffer.writeBytes(encodedQuery)

        return buffer
    }
}
