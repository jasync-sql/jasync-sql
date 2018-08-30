package com.github.jasync.sql.db.column

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatterBuilder
import org.junit.Test
import java.sql.Timestamp
import kotlin.test.assertEquals

class TimestampEncoderDecoderSpec {

    val encoder = TimestampEncoderDecoder()
    val dateTime = DateTime()
            .withDate(2013, 12, 27)
            .withTime(8, 40, 50, 800)

    val result = "2013-12-27 08:40:50.800000"
    val formatter = DateTimeFormatterBuilder().appendPattern("Z").toFormatter()
    val resultWithTimezone = "2013-12-27 08:40:50.800000${formatter.print(dateTime)}"

    @Test
    fun `should print a timestamp`() {
        val timestamp = Timestamp(dateTime.toDate().time)
        assertEquals(encoder.encode(timestamp), resultWithTimezone)
    }

    @Test
    fun `should print a LocalDateTime`() {
        assertEquals(encoder.encode(dateTime.toLocalDateTime()), result)
    }

    @Test
    fun `should print a date`() {
        assertEquals(encoder.encode(dateTime.toDate()), resultWithTimezone)
    }

    @Test
    fun `should print a calendar`() {
        val calendar = java.util.Calendar.getInstance()
        calendar.time = dateTime.toDate()
        encoder.encode(calendar) === resultWithTimezone
    }

    @Test
    fun `should print a datetime`() {
        encoder.encode(dateTime) === resultWithTimezone
    }
}
