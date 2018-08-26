
package com.github.mauricio.async.db.mysql.binary.encoder

import io.netty.buffer.ByteBuf
import com.github.mauricio.async.db.mysql.column.ColumnTypes
import org.joda.time.*

object DateTimeEncoder : BinaryEncoder {
  override fun encode(value: Any, buffer: ByteBuf) {
    val instant = value as ReadableDateTime

    return LocalDateTimeEncoder.encode(LocalDateTime(instant.millis), buffer)
  }

  override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_TIMESTAMP

}
