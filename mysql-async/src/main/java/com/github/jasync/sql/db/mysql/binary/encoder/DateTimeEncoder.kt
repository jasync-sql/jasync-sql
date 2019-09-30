package com.github.jasync.sql.db.mysql.binary.encoder

import com.github.jasync.sql.db.mysql.column.ColumnTypes
import io.netty.buffer.ByteBuf
import java.time.ZonedDateTime

object DateTimeEncoder : BinaryEncoder {
    override fun encode(value: Any, buffer: ByteBuf) {
        val zdt = value as ZonedDateTime

        return LocalDateTimeEncoder.encode(zdt.toLocalDateTime(), buffer)
    }

    override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_TIMESTAMP

}
