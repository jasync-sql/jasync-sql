package com.github.mauricio.async.db.postgresql.parsers

import com.github.mauricio.async.db.postgresql.messages.backend.ReadyForQueryMessage
import com.github.mauricio.async.db.postgresql.messages.backend.ServerMessage
import io.netty.buffer.ByteBuf


object ReadyForQueryParser : MessageParser {

  override fun parseMessage(b: ByteBuf): ServerMessage {
    return ReadyForQueryMessage(b.readByte().toChar())
  }

}
