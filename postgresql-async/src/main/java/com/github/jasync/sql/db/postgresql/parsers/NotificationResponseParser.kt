package com.github.jasync.sql.db.postgresql.parsers

import com.github.jasync.sql.db.postgresql.messages.backend.NotificationResponse
import com.github.jasync.sql.db.postgresql.messages.backend.ServerMessage
import com.github.jasync.sql.db.util.readCString
import io.netty.buffer.ByteBuf
import java.nio.charset.Charset

class NotificationResponseParser(val charset: Charset) : MessageParser {

    override fun parseMessage(buffer: ByteBuf): ServerMessage {
        return NotificationResponse(buffer.readInt(), buffer.readCString(charset), buffer.readCString(charset))
    }

}
