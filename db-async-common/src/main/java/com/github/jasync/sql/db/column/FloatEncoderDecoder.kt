package com.github.jasync.sql.db.column

object FloatEncoderDecoder : ColumnEncoderDecoder {
    override fun decode(value: String): Float = value.toFloat()
}
