package com.github.jasync.sql.db.mysql.binary.encoder

import com.github.jasync.sql.db.mysql.column.ColumnTypes
import com.github.jasync.sql.db.util.writeLengthEncodedString
import io.netty.buffer.ByteBuf
import java.nio.charset.Charset

class StringEncoder(val charset: Charset) : BinaryEncoder {

    override fun encode(value: Any, buffer: ByteBuf) {
        buffer.writeLengthEncodedString(value.toString(), charset)
    }

    override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_VARCHAR
}
