package com.github.jasync.sql.db.column

object ShortEncoderDecoder : ColumnEncoderDecoder {

    override fun decode(value: String): Any = value.toShort()

}
