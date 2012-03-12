package com.github.mauricio.postgresql.column


/**
 * User: Maurício Linhares
 * Date: 3/5/12
 * Time: 9:50 AM
 */

object BooleanEncoderDecoder extends ColumnEncoderDecoder {

  override def decode(value: String): Any = {
    if ( "t" == value ) {
      true
    } else {
      false
    }
  }

  override def encode( value : Any ) : String = {
    val result = value.asInstanceOf[Boolean]

    if (result) {
      "t"
    } else {
      "f"
    }
  }

}
