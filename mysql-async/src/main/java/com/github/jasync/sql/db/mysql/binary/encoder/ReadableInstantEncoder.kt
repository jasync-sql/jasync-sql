package com.github.jasync.sql.db.mysql.binary.encoder

import com.github.jasync.sql.db.mysql.column.ColumnTypes
import io.netty.buffer.ByteBuf
import org.joda.time.LocalDateTime
import org.joda.time.ReadableInstant

object ReadableInstantEncoder : BinaryEncoder {
    override fun encode(value: Any, buffer: ByteBuf) {
        val date = value as ReadableInstant
        LocalDateTimeEncoder.encode(LocalDateTime(date.millis), buffer)
    }

    override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_TIMESTAMP
}
