package com.github.jasync.sql.db.column

import java.time.format.DateTimeFormatter

object TimeWithTimezoneEncoderDecoder : TimeEncoderDecoder() {

    private val format = DateTimeFormatter.ofPattern("HH:mm:ss.SSSSSSZ")

    override fun formatter(): DateTimeFormatter = format

}
