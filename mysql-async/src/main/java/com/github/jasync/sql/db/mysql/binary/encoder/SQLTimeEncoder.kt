package com.github.jasync.sql.db.mysql.binary.encoder

import com.github.jasync.sql.db.mysql.column.ColumnTypes
import io.netty.buffer.ByteBuf
import org.joda.time.LocalTime

object SQLTimeEncoder : BinaryEncoder {
    override fun encode(value: Any, buffer: ByteBuf) {
        val sqlTime = (value as java.sql.Time).time
        val time = LocalTime(sqlTime)
        LocalTimeEncoder.encode(time, buffer)
    }

    override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_TIME
}
