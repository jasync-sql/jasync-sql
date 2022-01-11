package com.github.jasync.sql.db.column

object IntegerEncoderDecoder : ColumnEncoderDecoder {

    override fun decode(value: String): Int = value.toInt()
}
