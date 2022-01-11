package com.github.jasync.sql.db.mysql.binary.encoder

import com.github.jasync.sql.db.mysql.column.ColumnTypes
import io.netty.buffer.ByteBuf

object SQLDateEncoder : BinaryEncoder {
    override fun encode(value: Any, buffer: ByteBuf) {
        val date = value as java.sql.Date

        LocalDateEncoder.encode(date.toLocalDate(), buffer)
    }

    override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_DATE
}
