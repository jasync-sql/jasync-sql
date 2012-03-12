package com.github.mauricio.postgresql.column

import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime

/**
 * User: Maurício Linhares
 * Date: 3/6/12
 * Time: 9:27 AM
 */

object TimestampWithTimezoneEncoderDecoder extends ColumnEncoderDecoder {

  private val parser = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSSSSSZ")

  override def decode(value: String): DateTime = {
    parser.parseDateTime(value)
  }

  override def encode( value : Any ) : String = {
    this.parser.print( value.asInstanceOf[DateTime] )
  }

}
