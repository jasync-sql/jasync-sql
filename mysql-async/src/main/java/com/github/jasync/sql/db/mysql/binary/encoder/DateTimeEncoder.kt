package com.github.jasync.sql.db.mysql.binary.encoder

import com.github.jasync.sql.db.mysql.column.ColumnTypes
import io.netty.buffer.ByteBuf
import org.joda.time.LocalDateTime
import org.joda.time.ReadableDateTime

object DateTimeEncoder : BinaryEncoder {
    override fun encode(value: Any, buffer: ByteBuf) {
        val instant = value as ReadableDateTime

        return LocalDateTimeEncoder.encode(LocalDateTime(instant.millis), buffer)
    }

    override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_TIMESTAMP
}
