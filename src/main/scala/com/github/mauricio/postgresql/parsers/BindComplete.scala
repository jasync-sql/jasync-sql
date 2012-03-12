package com.github.mauricio.postgresql.parsers

import com.github.mauricio.postgresql.Message

/**
 * User: Maurício Linhares
 * Date: 3/12/12
 * Time: 2:32 AM
 */

object BindComplete {
  val Instance = new BindComplete()
}


class BindComplete extends Message( Message.BindComplete, true )
