package com.github.jasync.sql.db.column

import com.github.jasync.sql.db.util.microsecondsFormatter
import java.time.LocalTime
import java.time.format.DateTimeFormatterBuilder

open class TimeEncoderDecoder : ColumnEncoderDecoder {
    companion object {
        val Instance = TimeEncoderDecoder()
    }

    private val format = DateTimeFormatterBuilder()
        .appendPattern("HH:mm:ss")
        .appendOptional(microsecondsFormatter)
        .toFormatter()

    private val printer = DateTimeFormatterBuilder()
        .appendPattern("HH:mm:ss.SSSSSS")
        .toFormatter()

    open fun formatter() = format

    override fun decode(value: String): LocalTime =
        LocalTime.parse(value, formatter())

    override fun encode(value: Any): String =
        (value as LocalTime).format(printer)
}
