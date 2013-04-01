package com.github.mauricio.postgresql.parsers

import com.github.mauricio.postgresql.messages.backend.{ErrorMessage, Message}

object ErrorParser extends InformationParser {

  def createMessage(fields: Map[Char, String]): Message = new ErrorMessage(fields)

}