
package com.github.mauricio.async.db.mysql.binary.encoder

import io.netty.buffer.ByteBuf
import java.util.Calendar
import org.joda.time.LocalDateTime
import org.joda.time.DateTime
import com.github.mauricio.async.db.mysql.column.ColumnTypes

object CalendarEncoder : BinaryEncoder {
  override fun encode(value: Any, buffer: ByteBuf) {
    val calendar = value as Calendar
    LocalDateTimeEncoder.encode(LocalDateTime(calendar.timeInMillis), buffer)
  }

  override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_TIMESTAMP

}
