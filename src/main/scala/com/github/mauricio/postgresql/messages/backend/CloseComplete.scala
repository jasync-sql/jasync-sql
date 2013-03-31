package com.github.mauricio.postgresql.messages.backend


/**
 * User: Maurício Linhares
 * Date: 3/12/12
 * Time: 11:37 PM
 */

object CloseComplete {
  val Instance = new CloseComplete()
}

class CloseComplete extends Message(Message.CloseComplete)