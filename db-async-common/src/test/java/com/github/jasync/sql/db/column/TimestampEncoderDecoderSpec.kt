/*
 * Copyright 2013 Maurício Linhares
 *
 * Maurício Linhares licenses this file to you under the Apache License,
 * version 2.0 (the `License`); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an `AS IS` BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

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
