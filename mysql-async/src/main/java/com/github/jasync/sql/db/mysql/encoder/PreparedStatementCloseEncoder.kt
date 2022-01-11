package com.github.jasync.sql.db.mysql.encoder

import com.github.jasync.sql.db.mysql.message.client.ClientMessage
import com.github.jasync.sql.db.mysql.message.client.CloseStatementMessage
import com.github.jasync.sql.db.util.ByteBufferUtils
import io.netty.buffer.ByteBuf

object PreparedStatementCloseEncoder : MessageEncoder {

    override fun encode(message: ClientMessage): ByteBuf {
        val m = message as CloseStatementMessage
        val buffer = ByteBufferUtils.packetBuffer(5)
        buffer.writeByte(ClientMessage.PreparedStatementClose)
        buffer.writeBytes(m.statementId)

        return buffer
    }
}
