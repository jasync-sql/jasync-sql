package com.github.mauricio.postgresql.parsers

import com.github.mauricio.postgresql.messages.backend.{NoticeMessage, Message}

/**
 * User: Maurício Linhares
 * Date: 3/1/12
 * Time: 10:06 PM
 */

object NoticeParser extends InformationParser {

  def createMessage(fields: Map[Char, String]): Message = new NoticeMessage(fields)

}
