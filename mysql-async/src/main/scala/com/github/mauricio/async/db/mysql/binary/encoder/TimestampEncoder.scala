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

package com.github.mauricio.async.db.mysql.binary.encoder

import org.jboss.netty.buffer.ChannelBuffer
import org.joda.time._
import com.github.mauricio.async.db.exceptions.DateEncoderNotAvailableException

object TimestampEncoder extends BinaryEncoder {
  def encode(value: Any, buffer: ChannelBuffer) {

    val instant = value match {
      case v : ReadableDateTime => v
      case v : ReadableInstant => new DateTime(v.getMillis)
      case v : LocalDateTime => v.toDateTime( DateTimeZone.UTC )
      case v : java.util.Date => new DateTime(v)
      case v : java.sql.Timestamp => new DateTime(v)
      case v : java.util.Calendar => new DateTime(v)
      case _ => throw new DateEncoderNotAvailableException(value)
    }

    buffer.writeByte(11)
    buffer.writeShort(instant.getYear)
    buffer.writeByte(instant.getMonthOfYear)
    buffer.writeByte(instant.getDayOfMonth)
    buffer.writeByte(instant.getHourOfDay)
    buffer.writeByte(instant.getMinuteOfHour)
    buffer.writeByte(instant.getSecondOfMinute)
    buffer.writeInt(instant.getMillisOfDay)

  }
}
