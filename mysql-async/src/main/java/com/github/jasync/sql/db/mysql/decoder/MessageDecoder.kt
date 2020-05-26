package com.github.jasync.sql.db.mysql.decoder

import com.github.jasync.sql.db.mysql.message.server.ServerMessage
import io.netty.buffer.ByteBuf

interface MessageDecoder {

    fun decode(buffer: ByteBuf): ServerMessage
}
