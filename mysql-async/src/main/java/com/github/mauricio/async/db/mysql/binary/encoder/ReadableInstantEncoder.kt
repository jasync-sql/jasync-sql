
package com.github.mauricio.async.db.mysql.binary.encoder

import io.netty.buffer.ByteBuf
import com.github.mauricio.async.db.mysql.column.ColumnTypes
import org.joda.time.*

object ReadableInstantEncoder : BinaryEncoder {
  override fun encode(value: Any, buffer: ByteBuf) {
    val date = value as ReadableInstant
    LocalDateTimeEncoder.encode(LocalDateTime(date.millis), buffer)
  }

  override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_TIMESTAMP
}
