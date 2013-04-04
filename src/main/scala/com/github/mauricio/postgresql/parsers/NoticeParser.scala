package com.github.mauricio.postgresql.parsers

import com.github.mauricio.postgresql.messages.backend.{NoticeMessage, Message}
import java.nio.charset.Charset

/**
 * User: Maurício Linhares
 * Date: 3/1/12
 * Time: 10:06 PM
 */

class NoticeParser( charset : Charset ) extends InformationParser(charset) {

  def createMessage(fields: Map[Char, String]): Message = new NoticeMessage(fields)

}
