package com.github.mauricio.async.db.postgresql.parsers

import com.github.jasync.sql.db.util.readCString
import com.github.mauricio.async.db.postgresql.messages.backend.NotificationResponse
import com.github.mauricio.async.db.postgresql.messages.backend.ServerMessage
import io.netty.buffer.ByteBuf
import java.nio.charset.Charset

class NotificationResponseParser(val charset: Charset) : MessageParser {

  override fun parseMessage(buffer: ByteBuf): ServerMessage {
    return NotificationResponse(buffer.readInt(), buffer.readCString(charset), buffer.readCString(charset))
  }

}
