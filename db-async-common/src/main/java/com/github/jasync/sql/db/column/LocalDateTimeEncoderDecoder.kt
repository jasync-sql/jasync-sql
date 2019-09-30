package com.github.jasync.sql.db.column

import java.time.LocalDateTime
import java.time.format.DateTimeFormatterBuilder

object LocalDateTimeEncoderDecoder : ColumnEncoderDecoder {

    private const val ZeroedTimestamp = "0000-00-00 00:00:00"

    private val optional = DateTimeFormatterBuilder()
        .appendPattern(".SSSSSS").toFormatter()

    private val format = DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd HH:mm:ss")
        .appendOptional(optional)
        .toFormatter()

    override fun encode(value: Any): String = (value as LocalDateTime).format(format)

    override fun decode(value: String): LocalDateTime? =
        if (ZeroedTimestamp == value) {
            null
        } else {
            LocalDateTime.parse(value, format)
        }

}
