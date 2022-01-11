package com.github.jasync.sql.db.postgresql.column

import com.github.jasync.sql.db.column.ColumnEncoderDecoder

object CharEncoderDecoder : ColumnEncoderDecoder {
    override fun decode(value: String): Any = value.toCharArray().first()
}
