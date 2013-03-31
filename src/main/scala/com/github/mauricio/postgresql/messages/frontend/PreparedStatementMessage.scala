package com.github.mauricio.postgresql.messages.frontend

import com.github.mauricio.postgresql.column.ColumnEncoderDecoder

/**
 * User: Maurício Linhares
 * Date: 3/12/12
 * Time: 10:12 PM
 */

class PreparedStatementMessage( kind : Char, val query : String, val values : Seq[Any] ) extends FrontendMessage(kind) {

  val valueTypes : Seq[Int] = values.map {
    value =>
      if ( value == null ) {
        0
      } else {
        ColumnEncoderDecoder.kindFor(value.asInstanceOf[AnyRef].getClass)
      }

  }

}