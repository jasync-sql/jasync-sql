package com.github.jasync.sql.db.mysql.binary.encoder

import com.github.jasync.sql.db.mysql.column.ColumnTypes
import io.netty.buffer.ByteBuf
import java.time.LocalDate

object LocalDateEncoder : BinaryEncoder {
    override fun encode(value: Any, buffer: ByteBuf) {
        val date = value as LocalDate

        buffer.writeByte(4)
        buffer.writeShort(date.year)
        buffer.writeByte(date.monthValue)
        buffer.writeByte(date.dayOfMonth)

    }

    override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_DATE
}
