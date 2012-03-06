package com.github.mauricio.postgresql.column

import com.github.mauricio.postgresql.util.Log

/**
 * User: Maurício Linhares
 * Date: 3/5/12
 * Time: 9:50 AM
 */

object BooleanDecoder extends ColumnDecoder {

  def decode(value: String): Any = {
    if ( "t" == value ) {
      true
    } else {
      false
    }
  }

}
