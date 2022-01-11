package com.github.jasync.sql.db.postgresql.encoders

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

object SSLMessageEncoder {

    fun encode(): ByteBuf {
        val buffer = Unpooled.buffer()
        buffer.writeInt(8)
        buffer.writeShort(1234)
        buffer.writeShort(5679)
        return buffer
    }
}
