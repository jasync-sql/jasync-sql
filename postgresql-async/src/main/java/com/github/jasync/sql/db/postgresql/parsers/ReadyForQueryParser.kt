package com.github.jasync.sql.db.postgresql.parsers

import com.github.jasync.sql.db.postgresql.messages.backend.ReadyForQueryMessage
import com.github.jasync.sql.db.postgresql.messages.backend.ServerMessage
import io.netty.buffer.ByteBuf

object ReadyForQueryParser : MessageParser {

    override fun parseMessage(buffer: ByteBuf): ServerMessage {
        return ReadyForQueryMessage(buffer.readByte().toChar())
    }
}
