package com.github.mauricio.postgresql.column

import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat

/**
 * User: Maurício Linhares
 * Date: 3/5/12
 * Time: 6:12 PM
 */

object DateEncoderDecoder extends ColumnEncoderDecoder {

  private val parser = DateTimeFormat.forPattern("yyyy-MM-dd")

  override def decode(value: String): LocalDate = {
    this.parser.parseLocalDate(value)
  }

  override def encode( value : Any ) : String = {
    this.parser.print( value.asInstanceOf[LocalDate] )
  }

}
