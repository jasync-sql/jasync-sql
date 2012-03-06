package com.github.mauricio.postgresql.column

/**
 * User: Maurício Linhares
 * Date: 3/5/12
 * Time: 9:45 AM
 */

object StringDecoder extends ColumnDecoder {
  def decode(value: String): String = {
    value
  }
}
