package com.github.mauricio.postgresql.parsers

import com.github.mauricio.postgresql.Message

/**
 * User: Maurício Linhares
 * Date: 3/12/12
 * Time: 11:37 PM
 */

object CloseComplete {
  val Instance = new CloseComplete()
}

class CloseComplete extends Message(Message.CloseComplete, true) {

}
