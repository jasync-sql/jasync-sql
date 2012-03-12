package com.github.mauricio.postgresql.column

/**
 * User: Maurício Linhares
 * Date: 3/5/12
 * Time: 9:42 AM
 */

object BigDecimalEncoderDecoder extends ColumnEncoderDecoder {

  def decode( value : String ) : Any = {
    BigDecimal(value)
  }

}
