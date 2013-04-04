package com.github.mauricio.postgresql.column

import org.joda.time.{ReadableInstant, LocalDate}
import org.joda.time.format.DateTimeFormat
import com.github.mauricio.postgresql.exceptions.DateEncoderNotAvailableException

/**
 * User: Maurício Linhares
 * Date: 3/5/12
 * Time: 6:12 PM
 */

object DateEncoderDecoder extends ColumnEncoderDecoder {

  private val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")

  override def decode(value: String): LocalDate = {
    this.formatter.parseLocalDate(value)
  }

  override def encode( value : Any ) : String = {
    value match {
      case d : java.sql.Date => this.formatter.print( new LocalDate(d) )
      case d : ReadableInstant => this.formatter.print(d)
      case _ => throw new DateEncoderNotAvailableException(value)
    }
  }

}
