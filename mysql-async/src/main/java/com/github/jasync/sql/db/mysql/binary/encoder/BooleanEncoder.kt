package com.github.jasync.sql.db.mysql.binary.encoder

import com.github.jasync.sql.db.mysql.column.ColumnTypes
import io.netty.buffer.ByteBuf

object BooleanEncoder : BinaryEncoder {
    override fun encode(value: Any, buffer: ByteBuf) {
        val boolean = value as Boolean
        if (boolean) {
            buffer.writeByte(1)
        } else {
            buffer.writeByte(0)
        }
    }

    override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_TINY
}
