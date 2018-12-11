package com.github.jasync.sql.db.mysql.binary.encoder

import com.github.jasync.sql.db.mysql.column.ColumnTypes
import io.netty.buffer.ByteBuf

object LongEncoder : BinaryEncoder {
    override fun encode(value: Any, buffer: ByteBuf) {
        buffer.writeLong(value as Long)
    }

    override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_LONGLONG
}
