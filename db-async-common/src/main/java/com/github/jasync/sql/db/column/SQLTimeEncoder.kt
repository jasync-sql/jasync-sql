package com.github.jasync.sql.db.column

import org.joda.time.LocalTime
import org.joda.time.format.DateTimeFormatterBuilder

object SQLTimeEncoder : ColumnEncoder {

    private val format = DateTimeFormatterBuilder()
        .appendPattern("HH:mm:ss")
        .toFormatter()

    override fun encode(value: Any): String {
        val time = value as java.sql.Time

        return format.print(LocalTime(time.time))
    }
}
