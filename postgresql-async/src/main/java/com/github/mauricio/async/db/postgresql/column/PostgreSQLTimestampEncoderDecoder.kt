
package com.github.mauricio.async.db.postgresql.column

import com.github.jasync.sql.db.column.ColumnEncoderDecoder
import com.github.mauricio.async.db.column.ColumnEncoderDecoder
import com.github.mauricio.async.db.exceptions.DateEncoderNotAvailableException
import com.github.mauricio.async.db.general.ColumnData
import com.github.mauricio.async.db.postgresql.messages.backend.PostgreSQLColumnData
import com.github.mauricio.async.db.util.Log
import io.netty.buffer.ByteBuf
import java.nio.charset.Charset
import java.sql.Timestamp
import java.util.Calendar
import java.util.Date
import org.joda.time.*
import org.joda.time.format.DateTimeFormatterBuilder

object PostgreSQLTimestampEncoderDecoder : ColumnEncoderDecoder {

  private val log = Log.getByName(this.getClass.getName)

  private val optionalTimeZone = DateTimeFormatterBuilder()
    .appendPattern("Z").toParser

  private val internalFormatters = 1.until(6).inclusive.map {
    index ->
      DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd HH:mm:ss")
        .appendPattern("." + ("S" * index ))
        .appendOptional(optionalTimeZone)
        .toFormatter
  }

  private val internalFormatterWithoutSeconds = DateTimeFormatterBuilder()
    .appendPattern("yyyy-MM-dd HH:mm:ss")
    .appendOptional(optionalTimeZone)
    .toFormatter

  fun formatter ()= internalFormatters(5)

  override fun decode( kind : ColumnData, value : ByteBuf, charset : Charset ) : Any {
    val bytes = ByteArray(value.readableBytes())
    value.readBytes(bytes)

    val text = String(bytes, charset)

    val columnType = kind as PostgreSQLColumnData>

    columnType.dataType when {
      ColumnTypes.Timestamp | ColumnTypes.TimestampArray -> {
        selectFormatter(text).parseLocalDateTime(text)
      }
      ColumnTypes.TimestampWithTimezoneArray -> {
        selectFormatter(text).parseDateTime(text)
      }
      ColumnTypes.TimestampWithTimezone -> {
        if ( columnType.dataTypeModifier > 0 ) {
          internalFormatters(columnType.dataTypeModifier - 1).parseDateTime(text)
        } else {
          selectFormatter(text).parseDateTime(text)
        }
      }
    }
  }

  private fun selectFormatter( text : String ) {
    if ( text.contains(".") ) {
      internalFormatters(5)
    } else {
      internalFormatterWithoutSeconds
    }
  }

  override fun decode(value : String) : Any = throw UnsupportedOperationException("this method should not have been called")

  override fun encode(value: Any): String {
    value when {
      t: Timestamp -> this.formatter.print(DateTime(t))
      t: Date -> this.formatter.print(DateTime(t))
      t: Calendar -> this.formatter.print(DateTime(t))
      t: LocalDateTime -> this.formatter.print(t)
      t: ReadableDateTime -> this.formatter.print(t)
      else -> throw DateEncoderNotAvailableException(value)
    }
  }

  override fun supportsStringDecoding (): Boolean = false

}