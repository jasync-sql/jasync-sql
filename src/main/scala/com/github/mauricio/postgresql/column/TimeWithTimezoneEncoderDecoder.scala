package com.github.mauricio.postgresql.column

import org.joda.time.format.DateTimeFormat

/**
 * User: Maurício Linhares
 * Date: 3/11/12
 * Time: 5:35 PM
 */

object TimeWithTimezoneEncoderDecoder extends TimeEncoderDecoder {

  private val format = DateTimeFormat.forPattern("HH:mm:ss.SSSSSSZ")

  override def formatter = format

}
