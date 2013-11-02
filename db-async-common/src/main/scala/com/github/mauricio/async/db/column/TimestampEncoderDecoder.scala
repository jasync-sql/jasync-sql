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

import com.github.mauricio.async.db.exceptions.DateEncoderNotAvailableException
import java.sql.Timestamp
import java.util.{Calendar, Date}
import org.joda.time._
import org.joda.time.format.DateTimeFormatterBuilder

object TimestampEncoderDecoder {
  val Instance = new TimestampEncoderDecoder()
}

class TimestampEncoderDecoder extends ColumnEncoderDecoder {

  private val optional = new DateTimeFormatterBuilder()
    .appendPattern(".SSSSSS").toParser
  private val optionalTimeZone = new DateTimeFormatterBuilder()
    .appendPattern("Z").toParser

  private val format = new DateTimeFormatterBuilder()
    .appendPattern("yyyy-MM-dd HH:mm:ss")
    .appendOptional(optional)
    .appendOptional(optionalTimeZone)
    .toFormatter

  def formatter = format

  override def decode(value: String): Any = {
    formatter.parseLocalDateTime(value)
  }

  override def encode(value: Any): String = {
    value match {
      case t: Timestamp => this.formatter.print(new DateTime(t))
      case t: Date => this.formatter.print(new DateTime(t))
      case t: Calendar => this.formatter.print(new DateTime(t))
      case t: LocalDateTime => this.formatter.print(t)
      case t: ReadableDateTime => this.formatter.print(t)
      case _ => throw new DateEncoderNotAvailableException(value)
    }
  }

}
