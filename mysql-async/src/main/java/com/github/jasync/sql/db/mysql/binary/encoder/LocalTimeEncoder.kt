package com.github.jasync.sql.db.mysql.binary.encoder

import com.github.jasync.sql.db.mysql.column.ColumnTypes
import io.netty.buffer.ByteBuf
import java.time.LocalTime

object LocalTimeEncoder : BinaryEncoder {
    override fun encode(value: Any, buffer: ByteBuf) {
        val time = value as LocalTime

        val hasMillis = time.nano != 0

        if (hasMillis) {
            buffer.writeByte(12)
        } else {
            buffer.writeByte(8)
        }

        if (time.toNanoOfDay() > 0) {
            buffer.writeByte(0)
        } else {
            buffer.writeByte(1)
        }

        buffer.writeInt(0)

        buffer.writeByte(time.hour)
        buffer.writeByte(time.minute)
        buffer.writeByte(time.second)

        if (hasMillis) {
            buffer.writeInt(time.nano * 1000)
        }

    }

    override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_TIME
}
