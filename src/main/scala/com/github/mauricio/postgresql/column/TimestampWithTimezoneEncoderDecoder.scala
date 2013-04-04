package com.github.mauricio.postgresql.column

import org.joda.time.format.DateTimeFormat

/**
 * User: Maurício Linhares
 * Date: 3/6/12
 * Time: 9:27 AM
 */

object TimestampWithTimezoneEncoderDecoder extends TimestampEncoderDecoder {

  private val format = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSSSSSZ")

  override def formatter = format

}
