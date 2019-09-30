package com.github.jasync.sql.db.column

import java.time.LocalTime
import java.time.format.DateTimeFormatterBuilder

object SQLTimeEncoder : ColumnEncoder {

    private val format = DateTimeFormatterBuilder()
        .appendPattern("HH:mm:ss")
        .toFormatter()

    override fun encode(value: Any): String = (value as java.sql.Time).toLocalTime().format(format)
}
