package com.github.jasync.sql.db.column

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder


open class TimeEncoderDecoder : ColumnEncoderDecoder {
    companion object {
        val Instance = TimeEncoderDecoder()
    }

    private val optional = DateTimeFormatterBuilder()
        .appendPattern(".SSSSSS").toFormatter()

    private val format = DateTimeFormatterBuilder()
        .appendPattern("HH:mm:ss")
        .appendOptional(optional)
        .toFormatter()

    private val printer = DateTimeFormatterBuilder()
        .appendPattern("HH:mm:ss.SSSSSS")
        .toFormatter()

    open fun formatter(): DateTimeFormatter = format

    override fun decode(value: String): LocalTime = LocalTime.parse(value, format)

    override fun encode(value: Any): String = (value as LocalTime).format(printer)

}
