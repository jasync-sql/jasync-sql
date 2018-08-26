
package com.github.mauricio.async.db.mysql.binary.encoder

import io.netty.buffer.ByteBuf
import org.joda.time.LocalDateTime
import org.joda.time.DateTime
import com.github.mauricio.async.db.mysql.column.ColumnTypes

object JavaDateEncoder : BinaryEncoder {
  override fun encode(value: Any, buffer: ByteBuf) {
    val date = value as java.util.Date
    LocalDateTimeEncoder.encode(LocalDateTime(date.time), buffer)
  }

  override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_TIMESTAMP
}
