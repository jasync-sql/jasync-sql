package com.github.mauricio.async.db.postgresql.parsers

import com.github.mauricio.async.db.postgresql.messages.backend.NoticeMessage
import com.github.mauricio.async.db.postgresql.messages.backend.ServerMessage
import java.nio.charset.Charset

class NoticeParser(charset: Charset) : InformationParser(charset) {
  fun createMessage(fields: Map<Char, String>): ServerMessage = NoticeMessage(fields)
}
