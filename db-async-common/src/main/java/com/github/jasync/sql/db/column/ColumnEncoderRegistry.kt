package com.github.jasync.sql.db.column

interface ColumnEncoderRegistry {

    fun encode(value: Any?): String?

    fun kindOf(value: Any?): Int

}
