package com.github.jasync.sql.db.mysql.binary.encoder

import com.github.jasync.sql.db.mysql.column.ColumnTypes
import io.netty.buffer.ByteBuf
import java.time.LocalDateTime

object LocalDateTimeEncoder : BinaryEncoder {

    override fun encode(value: Any, buffer: ByteBuf) {
        val instant = value as LocalDateTime

        val hasNano = instant.nano != 0

        if (hasNano) {
            buffer.writeByte(11)
        } else {
            buffer.writeByte(7)
        }

        buffer.writeShort(instant.year)
        buffer.writeByte(instant.monthValue)
        buffer.writeByte(instant.dayOfMonth)
        buffer.writeByte(instant.hour)
        buffer.writeByte(instant.minute)
        buffer.writeByte(instant.second)

        if (hasNano) {
            buffer.writeInt(instant.nano / 1000)
        }
    }

    override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_TIMESTAMP
}
