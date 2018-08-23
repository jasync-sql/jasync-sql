
package com.github.mauricio.async.db.column

import org.joda.time.LocalTime
import org.joda.time.format.DateTimeFormatterBuilder

open class TimeEncoderDecoder : ColumnEncoderDecoder {
  companion object {
  val Instance = TimeEncoderDecoder()

  }
  private val optional = DateTimeFormatterBuilder()
    .appendPattern(".SSSSSS").toParser()

  private val format = DateTimeFormatterBuilder()
    .appendPattern("HH:mm:ss")
    .appendOptional(optional)
    .toFormatter()

  private val printer = DateTimeFormatterBuilder()
    .appendPattern("HH:mm:ss.SSSSSS")
    .toFormatter()

  open fun formatter ()= format

  override fun decode(value: String): LocalTime =
    format.parseLocalTime(value)

  override fun encode(value: Any): String =
    this.printer.print(value as LocalTime)

}
