package com.github.jasync.sql.db.mysql.binary.encoder

import com.github.jasync.sql.db.mysql.column.ColumnTypes
import io.netty.buffer.ByteBuf

object FloatEncoder : BinaryEncoder {
    override fun encode(value: Any, buffer: ByteBuf) {
        buffer.writeFloat(value as Float)
    }

    override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_FLOAT
}
