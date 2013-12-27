/*
 * Copyright 2013 Maurício Linhares
 *
 * Maurício Linhares licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.github.mauricio.async.db.column

import org.specs2.mutable.Specification
import org.joda.time.DateTime
import java.sql.Timestamp
import org.joda.time.format.DateTimeFormatterBuilder
import java.util.Calendar

class TimestampEncoderDecoderSpec extends Specification {

  val encoder = TimestampEncoderDecoder.Instance
  val dateTime = new DateTime()
    .withDate(2013, 12, 27)
    .withTime(8, 40, 50, 800)

  val result = "2013-12-27 08:40:50.800000"
  val formatter = new DateTimeFormatterBuilder().appendPattern("Z").toFormatter
  val resultWithTimezone = s"2013-12-27 08:40:50.800000${formatter.print(dateTime)}"

  "decoder" should {

    "should print a timestamp" in {
      val timestamp = new Timestamp(dateTime.toDate.getTime)
      encoder.encode(timestamp) === resultWithTimezone
    }

    "should print a LocalDateTime" in {
      encoder.encode(dateTime.toLocalDateTime) === result
    }

    "should print a date" in {
      encoder.encode(dateTime.toDate) === resultWithTimezone
    }

    "should print a calendar" in {
      val calendar = Calendar.getInstance()
      calendar.setTime(dateTime.toDate)
      encoder.encode(calendar) === resultWithTimezone
    }

    "should print a datetime" in {
      encoder.encode(dateTime) === resultWithTimezone
    }

  }

}