package com.github.mauricio.postgresql.messages

import com.github.mauricio.postgresql.{Message, FrontendMessage}
import com.github.mauricio.postgresql.column.ColumnEncoderDecoder


/**
 * User: Maurício Linhares
 * Date: 3/12/12
 * Time: 1:00 AM
 */

class PreparedStatementOpeningMessage( val query : String, val values : Seq[Any] ) extends FrontendMessage(Message.Parse) {

  val valueTypes : Seq[Int] = values.map {
    value =>
      if ( value == null ) {
        0
      } else {
        ColumnEncoderDecoder.kindFor(value.asInstanceOf[AnyRef].getClass)
      }

  }

}
