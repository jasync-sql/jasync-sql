package com.github.jasync.sql.db.column

import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

object TimeWithTimezoneEncoderDecoder : TimeEncoderDecoder() {

    private val format = DateTimeFormat.forPattern("HH:mm:ss.SSSSSSZ")

    override fun formatter(): DateTimeFormatter = format
}
