package com.github.jasync.sql.db.mysql.binary.encoder

import com.github.jasync.sql.db.mysql.column.ColumnTypes
import io.netty.buffer.ByteBuf

object DoubleEncoder : BinaryEncoder {
    override fun encode(value: Any, buffer: ByteBuf) {
        buffer.writeDouble(value as Double)
    }

    override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_DOUBLE
}
