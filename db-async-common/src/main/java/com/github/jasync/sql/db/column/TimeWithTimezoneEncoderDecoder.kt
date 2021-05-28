package com.github.jasync.sql.db.column

import com.github.jasync.sql.db.util.microsecondsFormatter
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder

object TimeWithTimezoneEncoderDecoder : TimeEncoderDecoder() {

    private val format = DateTimeFormatterBuilder().appendPattern("HH:mm:ss")
        .appendOptional(microsecondsFormatter)
        .appendPattern("[X][Z]")
        .toFormatter()

    override fun formatter(): DateTimeFormatter = format
}
