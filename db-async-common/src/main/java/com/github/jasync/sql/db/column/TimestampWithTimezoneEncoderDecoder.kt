package com.github.jasync.sql.db.column

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object TimestampWithTimezoneEncoderDecoder : TimestampEncoderDecoder() {

    private val format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSZ")

    override fun formatter(): DateTimeFormatter = format

    override fun decode(value: String): Any {
        return ZonedDateTime.parse(value, formatter())
    }

}
