package com.github.mauricio.async.db.postgresql.parsers

import com.github.mauricio.async.db.postgresql.messages.backend.ProcessData
import com.github.mauricio.async.db.postgresql.messages.backend.ServerMessage
import io.netty.buffer.ByteBuf


object BackendKeyDataParser : MessageParser {

  override fun parseMessage(b: ByteBuf): ServerMessage {
    return ProcessData(b.readInt(), b.readInt())
  }

}
