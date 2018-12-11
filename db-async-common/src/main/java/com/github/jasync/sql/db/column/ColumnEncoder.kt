package com.github.jasync.sql.db.column

interface ColumnEncoder {

    fun encode(value: Any): String = value.toString()

}
