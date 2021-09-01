package com.github.jasync.sql.db.column

import com.github.jasync.sql.db.util.microsecondsFormatter
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder

object TimestampWithTimezoneEncoderDecoder : TimestampEncoderDecoder() {

    private val format = DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd HH:mm:ss")
        .appendOptional(microsecondsFormatter)
        .appendPattern("[X][Z]")
        .toFormatter()

    override fun formatter(): DateTimeFormatter = format

    override fun decode(value: String): Any {
        return OffsetDateTime.parse(value, formatter())
    }
}
