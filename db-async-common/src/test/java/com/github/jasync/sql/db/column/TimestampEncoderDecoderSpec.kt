
package com.github.jasync.sql.db.column

import org.junit.Test
import java.sql.Timestamp
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatterBuilder
import java.util.*
import kotlin.test.assertEquals

class TimestampEncoderDecoderSpec {

    val encoder = TimestampEncoderDecoder()
    val dateTime = ZonedDateTime.of(2013, 12, 27,
            8, 40, 50, 800 * 1000000,
            ZoneId.systemDefault())

    val result = "2013-12-27 08:40:50.800000"
    val formatter = DateTimeFormatterBuilder().appendPattern("Z").toFormatter()
    val resultWithTimezone = "2013-12-27 08:40:50.800000${dateTime.format(formatter)}"

    @Test
    fun `should print a timestamp`() {

        Timestamp.from(dateTime.toInstant())
        val timestamp = Timestamp.from(dateTime.toInstant())
        assertEquals(encoder.encode(timestamp), resultWithTimezone)
    }

    @Test
    fun `should print a LocalDateTime`() {
        assertEquals(encoder.encode(dateTime.toLocalDateTime()), result)
    }

    @Test
    fun `should print a date`() {
        assertEquals(encoder.encode(Date.from(dateTime.toInstant())), resultWithTimezone)
    }

    @Test
    fun `should print a calendar`() {
        val calendar = Calendar.getInstance()
        calendar.time = Date.from(calendar.toInstant())
        encoder.encode(calendar) === resultWithTimezone
    }

    @Test
    fun `should print a datetime`() {
        encoder.encode(dateTime) === resultWithTimezone
    }
}