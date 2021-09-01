package com.github.jasync.sql.db.mysql.binary.encoder

import com.github.jasync.sql.db.mysql.column.ColumnTypes
import io.netty.buffer.ByteBuf

object SQLTimeEncoder : BinaryEncoder {
    override fun encode(value: Any, buffer: ByteBuf) {
        val time = (value as java.sql.Time).toLocalTime()
        LocalTimeEncoder.encode(time, buffer)
    }

    override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_TIME
}
