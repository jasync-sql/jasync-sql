package com.github.jasync.sql.db.mysql.encoder

import com.github.jasync.sql.db.mysql.message.client.ClientMessage
import com.github.jasync.sql.db.util.ByteBufferUtils
import io.netty.buffer.ByteBuf

object QuitMessageEncoder : MessageEncoder {

    override fun encode(message: ClientMessage): ByteBuf {
        val buffer = ByteBufferUtils.packetBuffer(5)
        buffer.writeByte(ClientMessage.Quit)
        return buffer
    }
}
