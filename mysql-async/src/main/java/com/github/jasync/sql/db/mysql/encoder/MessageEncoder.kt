package com.github.jasync.sql.db.mysql.encoder

import com.github.jasync.sql.db.mysql.message.client.ClientMessage
import io.netty.buffer.ByteBuf

interface MessageEncoder {

    fun encode(message: ClientMessage): ByteBuf
}
