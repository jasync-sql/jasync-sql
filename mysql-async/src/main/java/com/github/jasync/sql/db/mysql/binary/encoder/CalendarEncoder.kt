package com.github.jasync.sql.db.mysql.binary.encoder

import com.github.jasync.sql.db.mysql.column.ColumnTypes
import io.netty.buffer.ByteBuf
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

object CalendarEncoder : BinaryEncoder {
    override fun encode(value: Any, buffer: ByteBuf) {
        val calendar = value as Calendar
        LocalDateTimeEncoder.encode(LocalDateTime.ofInstant(calendar.toInstant(), ZoneOffset.UTC), buffer)
    }

    override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_TIMESTAMP

}
