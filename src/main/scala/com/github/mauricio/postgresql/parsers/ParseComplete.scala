package com.github.mauricio.postgresql.parsers

import com.github.mauricio.postgresql.Message

/**
 * User: Maurício Linhares
 * Date: 3/12/12
 * Time: 2:29 AM
 */

object ParseComplete {
  val Instance = new ParseComplete()
}

class ParseComplete extends Message(Message.ParseComplete, true)
