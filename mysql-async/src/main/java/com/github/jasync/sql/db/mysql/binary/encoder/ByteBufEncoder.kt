package com.github.jasync.sql.db.mysql.binary.encoder

import com.github.jasync.sql.db.mysql.column.ColumnTypes
import com.github.jasync.sql.db.util.writeLength
import io.netty.buffer.ByteBuf

object ByteBufEncoder : BinaryEncoder {
    override fun encode(value: Any, buffer: ByteBuf) {
        val bytes = value as ByteBuf

        buffer.writeLength(bytes.readableBytes().toLong())
        buffer.writeBytes(bytes)
    }

    override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_BLOB
}
