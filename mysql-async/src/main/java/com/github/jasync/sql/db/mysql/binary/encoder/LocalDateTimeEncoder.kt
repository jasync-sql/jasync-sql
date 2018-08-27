
package com.github.jasync.sql.db.mysql.binary.encoder

import io.netty.buffer.ByteBuf
import com.github.jasync.sql.db.mysql.column.ColumnTypes
import org.joda.time.*

object LocalDateTimeEncoder : BinaryEncoder {

  override fun encode(value: Any, buffer: ByteBuf) {
    val instant = value as LocalDateTime

    val hasMillis = instant.millisOfSecond != 0

    if ( hasMillis ) {
      buffer.writeByte(11)
    } else {
      buffer.writeByte(7)
    }

    buffer.writeShort(instant.year)
    buffer.writeByte(instant.monthOfYear)
    buffer.writeByte(instant.dayOfMonth)
    buffer.writeByte(instant.hourOfDay)
    buffer.writeByte(instant.minuteOfHour)
    buffer.writeByte(instant.secondOfMinute)

    if ( hasMillis ) {
      buffer.writeInt(instant.millisOfSecond * 1000)
    }

  }

  override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_TIMESTAMP
}
