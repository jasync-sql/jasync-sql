
package com.github.mauricio.async.db.mysql.binary.encoder

import io.netty.buffer.ByteBuf
import org.joda.time.LocalDate
import com.github.mauricio.async.db.mysql.column.ColumnTypes

object SQLDateEncoder : BinaryEncoder {
  override fun encode(value: Any, buffer: ByteBuf) {
    val date = value as java.sql.Date

    LocalDateEncoder.encode(LocalDate(date), buffer)
  }

  override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_DATE
}
