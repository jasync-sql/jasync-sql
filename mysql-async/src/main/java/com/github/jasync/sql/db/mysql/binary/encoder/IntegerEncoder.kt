package com.github.jasync.sql.db.mysql.binary.encoder

import com.github.jasync.sql.db.mysql.column.ColumnTypes
import io.netty.buffer.ByteBuf

object IntegerEncoder : BinaryEncoder {
    override fun encode(value: Any, buffer: ByteBuf) {
        buffer.writeInt(value as Int)
    }

    override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_LONG
}
