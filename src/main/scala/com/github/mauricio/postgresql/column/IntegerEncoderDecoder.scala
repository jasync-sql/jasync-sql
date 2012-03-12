package com.github.mauricio.postgresql.column

/**
 * User: Maurício Linhares
 * Date: 3/5/12
 * Time: 9:39 AM
 */

object IntegerEncoderDecoder extends ColumnEncoderDecoder {

  def decode(value: String): Int = {
    value.toInt
  }

}
