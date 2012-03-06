package com.github.mauricio.postgresql.column

/**
 * User: Maurício Linhares
 * Date: 3/5/12
 * Time: 9:55 AM
 */

object LongDecoder extends ColumnDecoder {
  def decode(value: String): Long = {
    value.toLong
  }
}
