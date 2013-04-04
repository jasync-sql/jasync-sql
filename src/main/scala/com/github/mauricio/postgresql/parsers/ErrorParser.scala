package com.github.mauricio.postgresql.parsers

import com.github.mauricio.postgresql.messages.backend.{ErrorMessage, Message}
import java.nio.charset.Charset

class ErrorParser( charset : Charset ) extends InformationParser(charset) {

  def createMessage(fields: Map[Char, String]): Message = new ErrorMessage(fields)

}