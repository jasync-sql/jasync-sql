package com.github.jasync.sql.db.column

import com.github.jasync.sql.db.util.microsecondsFormatter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatterBuilder

object LocalDateTimeEncoderDecoder : ColumnEncoderDecoder {

    private const val ZeroedTimestamp = "0000-00-00 00:00:00"

    private val format = DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd HH:mm:ss")
        .appendOptional(microsecondsFormatter)
        .toFormatter()

    override fun encode(value: Any): String =
        (value as LocalDateTime).format(format)

    override fun decode(value: String): LocalDateTime? =
        if (ZeroedTimestamp == value) {
            null
        } else {
            LocalDateTime.parse(value, format)
        }
}
