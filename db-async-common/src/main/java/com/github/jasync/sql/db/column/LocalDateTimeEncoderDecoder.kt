
package com.github.jasync.sql.db.column

import org.joda.time.format.DateTimeFormatterBuilder
import org.joda.time.LocalDateTime

object LocalDateTimeEncoderDecoder : ColumnEncoderDecoder {

  private val ZeroedTimestamp = "0000-00-00 00:00:00"

  private val optional = DateTimeFormatterBuilder()
    .appendPattern(".SSSSSS").toParser()

  private val format = DateTimeFormatterBuilder()
    .appendPattern("yyyy-MM-dd HH:mm:ss")
    .appendOptional(optional)
    .toFormatter()

  override fun encode(value: Any): String =
    format.print(value as LocalDateTime)

  override fun decode(value: String): LocalDateTime? =
    if (ZeroedTimestamp == value) {
      null
    } else {
      format.parseLocalDateTime(value)
    }

}
