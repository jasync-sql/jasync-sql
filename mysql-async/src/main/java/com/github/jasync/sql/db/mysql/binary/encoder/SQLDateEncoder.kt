package com.github.jasync.sql.db.mysql.binary.encoder

import com.github.jasync.sql.db.mysql.column.ColumnTypes
import io.netty.buffer.ByteBuf
import org.joda.time.LocalDate

object SQLDateEncoder : BinaryEncoder {
    override fun encode(value: Any, buffer: ByteBuf) {
        val date = value as java.sql.Date

        LocalDateEncoder.encode(LocalDate(date), buffer)
    }

    override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_DATE
}
