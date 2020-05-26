package com.github.jasync.sql.db.column

object StringEncoderDecoder : ColumnEncoderDecoder {
    override fun decode(value: String): String = value
}
