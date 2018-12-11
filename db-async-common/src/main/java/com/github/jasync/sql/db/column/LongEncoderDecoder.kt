package com.github.jasync.sql.db.column

object LongEncoderDecoder : ColumnEncoderDecoder {
    override fun decode(value: String): Long = value.toLong()
}
