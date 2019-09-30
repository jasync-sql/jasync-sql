package com.github.jasync.sql.db.mysql.binary.encoder

import com.github.jasync.sql.db.mysql.column.ColumnTypes
import io.netty.buffer.ByteBuf
import java.time.LocalDateTime

object LocalDateTimeEncoder : BinaryEncoder {

    override fun encode(value: Any, buffer: ByteBuf) {
        val localDateTime = (value as LocalDateTime)

        val hasMillis = localDateTime.nano != 0

        if (hasMillis) {
            buffer.writeByte(11)
        } else {
            buffer.writeByte(7)
        }

        buffer.writeShort(localDateTime.year)
        buffer.writeByte(localDateTime.monthValue)
        buffer.writeByte(localDateTime.dayOfMonth)
        buffer.writeByte(localDateTime.hour)
        buffer.writeByte(localDateTime.minute)
        buffer.writeByte(localDateTime.second)

        if (hasMillis) {
            buffer.writeInt(localDateTime.nano * 1000)
        }

    }

    override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_TIMESTAMP
}
