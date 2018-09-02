package com.github.mauricio.async.db.postgresql.parsers

import com.github.mauricio.async.db.postgresql.messages.backend.ServerMessage
import io.netty.buffer.ByteBuf

interface MessageParser {

  fun parseMessage(buffer: ByteBuf): ServerMessage

}