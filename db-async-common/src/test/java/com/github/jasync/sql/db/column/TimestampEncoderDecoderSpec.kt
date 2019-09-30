package com.github.jasync.sql.db.column

import org.junit.Test
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatterBuilder
import java.util.*
import kotlin.test.assertEquals

class TimestampEncoderDecoderSpec {

    val encoder = TimestampEncoderDecoder()

    val localDateTime = LocalDateTime.of(2013, 12, 27,
            8, 40, 50, 800)
    val utcInstant = localDateTime.toInstant(ZoneOffset.UTC)

    val legacyDate = Date.from(utcInstant)

    val result = "2013-12-27 08:40:50.800000"
    val formatter = DateTimeFormatterBuilder().appendPattern("Z").toFormatter()
    val resultWithTimezone = "2013-12-27 08:40:50.800000${localDateTime.format(formatter)}"

    @Test
    fun `should print a timestamp`() {
        val timestamp = Timestamp(utcInstant.toEpochMilli())
        assertEquals(encoder.encode(timestamp), resultWithTimezone)
    }

    @Test
    fun `should print a LocalDateTime`() {
        assertEquals(encoder.encode(localDateTime), result)
    }

    @Test
    fun `should print a date`() {
        assertEquals(encoder.encode(legacyDate), resultWithTimezone)
    }

    @Test
    fun `should print a calendar`() {
        val calendar = java.util.Calendar.getInstance()
        calendar.time = legacyDate
        encoder.encode(calendar) === resultWithTimezone
    }

    @Test
    fun `should print a datetime`() {
        encoder.encode(localDateTime) === resultWithTimezone
    }
}
