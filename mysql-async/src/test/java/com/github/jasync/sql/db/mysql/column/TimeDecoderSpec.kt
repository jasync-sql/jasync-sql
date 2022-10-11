package com.github.jasync.sql.db.mysql.column

import org.junit.Test
import java.time.Duration
import kotlin.test.assertEquals

class TimeDecoderSpec {

    @Test
    fun `handle a time`() {

        val time = "120:10:07"
        val duration = Duration.ofHours(120) + Duration.ofMinutes(10) + Duration.ofSeconds(7)
        assertEquals(duration, TimeDecoder.decode(time))
    }

    @Test
    fun `handle a time with millis`() {

        val time = "120:10:07.00098"
        val duration = Duration.ofHours(120) + Duration.ofMinutes(10) + Duration.ofSeconds(7) + Duration.ofMillis(98)
        assertEquals(duration, TimeDecoder.decode(time))
    }
}
