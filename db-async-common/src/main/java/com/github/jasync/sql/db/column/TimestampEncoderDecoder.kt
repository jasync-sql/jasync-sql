package com.github.jasync.sql.db.column

//import com.github.mauricio.sql.db.column.TimestampEncoderDecoder.BaseFormat
//import com.github.mauricio.sql.db.column.TimestampEncoderDecoder.MillisFormat
import com.github.jasync.sql.db.exceptions.DateEncoderNotAvailableException
import java.sql.Timestamp
import java.util.Calendar
import java.util.Date
import org.joda.time.*
import org.joda.time.format.DateTimeFormatterBuilder


open class TimestampEncoderDecoder : ColumnEncoderDecoder {
  companion object {
    val BaseFormat = "yyyy-MM-dd HH:mm:ss"
    val MillisFormat = ".SSSSSS"
    val Instance = TimestampEncoderDecoder()

  }

  private val optional = DateTimeFormatterBuilder()
      .appendPattern(MillisFormat).toParser()
  private val optionalTimeZone = DateTimeFormatterBuilder()
      .appendPattern("Z").toParser()

  private val builder = DateTimeFormatterBuilder()
      .appendPattern(BaseFormat)
      .appendOptional(optional)
      .appendOptional(optionalTimeZone)

  private val timezonedPrinter = DateTimeFormatterBuilder()
      .appendPattern("${BaseFormat}${MillisFormat}Z").toFormatter()

  private val nonTimezonedPrinter = DateTimeFormatterBuilder()
      .appendPattern("${BaseFormat}${MillisFormat}").toFormatter()

  private val format = builder.toFormatter()

  open fun formatter() = format

  override fun decode(value: String): Any {
    return formatter().parseLocalDateTime(value)
  }

  override fun encode(value: Any): String {
    return when (value) {
      is Timestamp -> this.timezonedPrinter.print(DateTime(value))
      is Date -> this.timezonedPrinter.print(DateTime(value))
      is Calendar -> this.timezonedPrinter.print(DateTime(value))
      is LocalDateTime -> this.nonTimezonedPrinter.print(value)
      is ReadableDateTime -> this.timezonedPrinter.print(value)
      else -> throw DateEncoderNotAvailableException(value)
    }
  }

}
