
package com.github.jasync.sql.db.mysql.binary.encoder

import io.netty.buffer.ByteBuf
import org.joda.time.LocalTime
import com.github.jasync.sql.db.mysql.column.ColumnTypes

object LocalTimeEncoder : BinaryEncoder {
  override fun encode(value: Any, buffer: ByteBuf) {
    val time = value as LocalTime

    val hasMillis = time.millisOfSecond != 0

    if ( hasMillis ) {
      buffer.writeByte(12)
    } else {
      buffer.writeByte(8)
    }

    if ( time.getMillisOfDay() > 0 ) {
      buffer.writeByte(0)
    } else {
      buffer.writeByte(1)
    }

    buffer.writeInt(0)

    buffer.writeByte(time.hourOfDay)
    buffer.writeByte(time.minuteOfHour)
    buffer.writeByte(time.secondOfMinute)

    if ( hasMillis ) {
      buffer.writeInt(time.millisOfSecond * 1000)
    }

  }

  override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_TIME
}
