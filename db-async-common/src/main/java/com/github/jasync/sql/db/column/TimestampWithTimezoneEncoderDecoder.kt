package com.github.jasync.sql.db.column

import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

object TimestampWithTimezoneEncoderDecoder : TimestampEncoderDecoder() {

    private val format = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSSSSSZ")

    override fun formatter(): DateTimeFormatter = format

    override fun decode(value: String): Any {
        return formatter().parseDateTime(value)
    }
}
