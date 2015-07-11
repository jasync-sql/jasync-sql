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

import org.joda.time.LocalTime
import org.joda.time.format.DateTimeFormatterBuilder

object TimeEncoderDecoder {
  val Instance = new TimeEncoderDecoder()
}

class TimeEncoderDecoder extends ColumnEncoderDecoder {

  final private val optional = new DateTimeFormatterBuilder()
    .appendPattern(".SSSSSS").toParser

  final private val format = new DateTimeFormatterBuilder()
    .appendPattern("HH:mm:ss")
    .appendOptional(optional)
    .toFormatter

  final private val printer = new DateTimeFormatterBuilder()
    .appendPattern("HH:mm:ss.SSSSSS")
    .toFormatter

  def formatter = format

  override def decode(value: String): LocalTime =
    format.parseLocalTime(value)

  override def encode(value: Any): String =
    this.printer.print(value.asInstanceOf[LocalTime])

}
