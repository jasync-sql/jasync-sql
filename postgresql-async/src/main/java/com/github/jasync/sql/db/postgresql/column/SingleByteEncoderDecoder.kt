package com.github.jasync.sql.db.postgresql.column

import com.github.jasync.sql.db.column.ColumnEncoderDecoder

@Suppress("unused")
object SingleByteEncoderDecoder : ColumnEncoderDecoder {

    override fun encode(value: Any): String {
        val byte = value as Byte
        return ByteArrayEncoderDecoder.encode(ByteArray(1) { byte })
    }

    override fun decode(value: String): Any {
        return ByteArrayEncoderDecoder.decode(value)[0]
    }

}
