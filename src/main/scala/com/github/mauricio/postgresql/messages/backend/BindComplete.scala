package com.github.mauricio.postgresql.messages.backend


/**
 * User: Maurício Linhares
 * Date: 3/12/12
 * Time: 2:32 AM
 */

object BindComplete {
  val Instance = new BindComplete()
}


class BindComplete extends Message( Message.BindComplete )
