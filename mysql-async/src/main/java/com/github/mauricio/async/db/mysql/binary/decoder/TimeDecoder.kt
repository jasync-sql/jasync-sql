package com.github.mauricio.async.db.mysql.binary.decoder

import com.github.jasync.sql.db.util.days
import com.github.jasync.sql.db.util.hours
import com.github.jasync.sql.db.util.micros
import com.github.jasync.sql.db.util.minutes
import com.github.jasync.sql.db.util.neg
import com.github.jasync.sql.db.util.seconds
import io.netty.buffer.ByteBuf
import java.time.Duration

object TimeDecoder : BinaryDecoder {
  override fun decode(buffer: ByteBuf): Duration {

    return when (buffer.readUnsignedByte()) {
      0.toShort() -> 0.seconds
      8.toShort() -> {

        val isNegative = buffer.readUnsignedByte() == 1.toShort()

        val duration = buffer.readUnsignedInt().days +
            buffer.readUnsignedByte().hours +
            buffer.readUnsignedByte().minutes +
            buffer.readUnsignedByte().seconds

        if (isNegative) {
          duration.neg()
        } else {
          duration
        }

      }
      12.toShort() -> {

        val isNegative = buffer.readUnsignedByte() == 1.toShort()

        val duration = buffer.readUnsignedInt().days +
            buffer.readUnsignedByte().hours +
            buffer.readUnsignedByte().minutes +
            buffer.readUnsignedByte().seconds +
            buffer.readUnsignedInt().micros

        if (isNegative) {
          duration.neg()
        } else {
          duration
        }

      }
      else -> TODO()
    }

  }
}
