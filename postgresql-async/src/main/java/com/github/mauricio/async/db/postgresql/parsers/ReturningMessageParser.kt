package com.github.mauricio.async.db.postgresql.parsers

import com.github.mauricio.async.db.postgresql.messages.backend.BindComplete
import com.github.mauricio.async.db.postgresql.messages.backend.CloseComplete
import com.github.mauricio.async.db.postgresql.messages.backend.EmptyQueryString
import com.github.mauricio.async.db.postgresql.messages.backend.NoData
import com.github.mauricio.async.db.postgresql.messages.backend.ParseComplete
import com.github.mauricio.async.db.postgresql.messages.backend.ServerMessage
import io.netty.buffer.ByteBuf

enum class ReturningMessageParser(val message: ServerMessage) : MessageParser {

  BindCompleteMessageParser(BindComplete),
  CloseCompleteMessageParser(CloseComplete),
  EmptyQueryStringMessageParser(EmptyQueryString),
  NoDataMessageParser(NoData),
  ParseCompleteMessageParser(ParseComplete);

  override fun parseMessage(buffer: ByteBuf): ServerMessage = this.message

}
