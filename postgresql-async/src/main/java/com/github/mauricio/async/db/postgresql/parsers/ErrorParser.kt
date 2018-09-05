package com.github.mauricio.async.db.postgresql.parsers

import com.github.mauricio.async.db.postgresql.messages.backend.ErrorMessage
import com.github.mauricio.async.db.postgresql.messages.backend.ServerMessage
import java.nio.charset.Charset


class ErrorParser(charset: Charset) : InformationParser(charset) {

  override fun createMessage(fields: Map<Char, String>): ServerMessage = ErrorMessage(fields)

}
