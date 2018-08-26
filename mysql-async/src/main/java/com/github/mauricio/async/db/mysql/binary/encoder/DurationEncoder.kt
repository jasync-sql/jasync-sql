
package com.github.mauricio.async.db.mysql.binary.encoder

import com.github.jasync.sql.db.util.days
import com.github.jasync.sql.db.util.hours
import com.github.jasync.sql.db.util.micros
import com.github.jasync.sql.db.util.minutes
import com.github.jasync.sql.db.util.seconds
import io.netty.buffer.ByteBuf
import com.github.mauricio.async.db.mysql.column.ColumnTypes
import java.time.Duration

object DurationEncoder : BinaryEncoder {

  private val Zero = 0.seconds

  override fun encode(value: Any, buffer: ByteBuf) {
    val duration = value as Duration

    val days = duration.toDays()
    val hoursDuration = duration - days.days
    val hours = hoursDuration.toHours()
    val minutesDuration = hoursDuration - hours.hours
    val minutes = minutesDuration.toMinutes()
    val secondsDuration = minutesDuration - minutes.minutes
    val seconds = secondsDuration.seconds
    val microsDuration = secondsDuration - seconds.seconds
    val micros = microsDuration.micros

    val hasMicros  = micros != 0L

    if ( hasMicros ) {
      buffer.writeByte(12)
    } else {
      buffer.writeByte(8)
    }

    if (duration > Zero) {
      buffer.writeByte(0)
    } else {
      buffer.writeByte(1)
    }

    buffer.writeInt(days as Int)
    buffer.writeByte(hours as Int)
    buffer.writeByte(minutes as Int)
    buffer.writeByte(seconds as Int)

    if ( hasMicros ) {
      buffer.writeInt(micros as Int)
    }

  }

  override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_TIME
}
