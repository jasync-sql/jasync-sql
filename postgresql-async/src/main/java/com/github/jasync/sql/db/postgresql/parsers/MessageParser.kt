package com.github.jasync.sql.db.postgresql.parsers

import com.github.jasync.sql.db.postgresql.messages.backend.ServerMessage
import io.netty.buffer.ByteBuf

interface MessageParser {

    fun parseMessage(buffer: ByteBuf): ServerMessage
}
