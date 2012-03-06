package com.github.mauricio.postgresql.column

import org.joda.time.format.DateTimeFormat
import org.joda.time.LocalTime

/**
 * User: Maurício Linhares
 * Date: 3/5/12
 * Time: 6:13 PM
 */

object TimeDecoder extends ColumnDecoder {

  val parser = DateTimeFormat.forPattern("HH:mm:ss.SSSSSS")

  def decode(value: String): LocalTime = {
    parser.parseLocalTime(value)
  }
}
