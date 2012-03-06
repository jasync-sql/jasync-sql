package com.github.mauricio.postgresql.column

import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime

/**
 * User: Maurício Linhares
 * Date: 3/5/12
 * Time: 6:10 PM
 */

object TimestampDecoder extends ColumnDecoder {

  val parser = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")

  def decode(value: String): DateTime = {
    parser.parseDateTime(value)
  }
}
