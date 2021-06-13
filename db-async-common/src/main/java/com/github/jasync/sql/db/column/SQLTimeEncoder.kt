package com.github.jasync.sql.db.column

import java.time.format.DateTimeFormatterBuilder

object SQLTimeEncoder : ColumnEncoder {

    private val format = DateTimeFormatterBuilder()
        .appendPattern("HH:mm:ss")
        .toFormatter()

    override fun encode(value: Any): String {
        val time = value as java.sql.Time

        return time.toLocalTime().format(format)
    }
}
