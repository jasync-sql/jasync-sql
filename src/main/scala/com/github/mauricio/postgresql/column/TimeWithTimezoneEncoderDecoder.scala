package com.github.mauricio.postgresql.column

import org.joda.time.LocalTime
import org.joda.time.format.DateTimeFormat

/**
 * User: Maurício Linhares
 * Date: 3/11/12
 * Time: 5:35 PM
 */

object TimeWithTimezoneEncoderDecoder extends ColumnEncoderDecoder {
  private val parser = DateTimeFormat.forPattern("HH:mm:ss.SSSSSSZ")

  override def decode(value: String): LocalTime = {
    parser.parseLocalTime(value)
  }

  override def encode( value : Any ) : String = {
    this.parser.print( value.asInstanceOf[LocalTime] )
  }

}
