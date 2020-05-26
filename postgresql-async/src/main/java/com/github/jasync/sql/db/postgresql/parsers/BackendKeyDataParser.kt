package com.github.jasync.sql.db.postgresql.parsers

import com.github.jasync.sql.db.postgresql.messages.backend.ProcessData
import com.github.jasync.sql.db.postgresql.messages.backend.ServerMessage
import io.netty.buffer.ByteBuf

object BackendKeyDataParser : MessageParser {

    override fun parseMessage(buffer: ByteBuf): ServerMessage {
        return ProcessData(buffer.readInt(), buffer.readInt())
    }
}
