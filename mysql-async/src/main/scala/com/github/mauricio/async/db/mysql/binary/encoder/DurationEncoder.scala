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

import io.netty.buffer.ByteBuf
import scala.concurrent.duration._
import com.github.mauricio.async.db.mysql.column.ColumnTypes

object DurationEncoder extends BinaryEncoder {

  private final val Zero = 0.seconds

  def encode(value: Any, buffer: ByteBuf) {
    val duration = value.asInstanceOf[Duration]

    val days = duration.toDays
    val hoursDuration = duration - days.days
    val hours = hoursDuration.toHours
    val minutesDuration = hoursDuration - hours.hours
    val minutes = minutesDuration.toMinutes
    val secondsDuration = minutesDuration - minutes.minutes
    val seconds = secondsDuration.toSeconds
    val microsDuration = secondsDuration - seconds.seconds
    val micros = microsDuration.toMicros

    val hasMicros  = micros != 0

    if ( hasMicros ) {
      buffer.writeByte(12)
    } else {
      buffer.writeByte(8)
    }

    if (duration > Zero) {
      buffer.writeByte(0)
    } else {
      buffer.writeByte(1)
    }

    buffer.writeInt(days.asInstanceOf[Int])
    buffer.writeByte(hours.asInstanceOf[Int])
    buffer.writeByte(minutes.asInstanceOf[Int])
    buffer.writeByte(seconds.asInstanceOf[Int])

    if ( hasMicros ) {
      buffer.writeInt(micros.asInstanceOf[Int])
    }

  }

  def encodesTo: Int = ColumnTypes.FIELD_TYPE_TIME
}
