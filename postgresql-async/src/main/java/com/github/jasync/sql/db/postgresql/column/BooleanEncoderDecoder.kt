package com.github.jasync.sql.db.postgresql.column

import com.github.jasync.sql.db.column.ColumnEncoderDecoder

object BooleanEncoderDecoder : ColumnEncoderDecoder {

    override fun decode(value: String): Boolean = "t" == value

    override fun encode(value: Any): String {
        val result = value as Boolean
        return if (result) {
            "t"
        } else {
            "f"
        }
    }
}
