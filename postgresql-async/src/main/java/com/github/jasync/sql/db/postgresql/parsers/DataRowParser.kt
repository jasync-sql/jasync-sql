package com.github.jasync.sql.db.postgresql.parsers

import com.github.jasync.sql.db.postgresql.messages.backend.DataRowMessage
import com.github.jasync.sql.db.postgresql.messages.backend.ServerMessage
import io.netty.buffer.ByteBuf

object DataRowParser : MessageParser {

    override fun parseMessage(buffer: ByteBuf): ServerMessage {
        val row = Array(buffer.readShort().toInt()) {
            val length = buffer.readInt()
            if (length == -1) {
                null
            } else {
                buffer.readRetainedSlice(length)
            }
        }
        return DataRowMessage(row)
    }
}
