package com.github.jasync.sql.db.mysql.binary.encoder

import com.github.jasync.sql.db.mysql.column.ColumnTypes
import io.netty.buffer.ByteBuf
import org.joda.time.LocalDateTime

object JavaDateEncoder : BinaryEncoder {
    override fun encode(value: Any, buffer: ByteBuf) {
        val date = value as java.util.Date
        LocalDateTimeEncoder.encode(LocalDateTime(date.time), buffer)
    }

    override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_TIMESTAMP
}
