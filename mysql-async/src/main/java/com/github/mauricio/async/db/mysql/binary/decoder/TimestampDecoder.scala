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

package com.github.mauricio.async.db.mysql.binary.decoder

import io.netty.buffer.ByteBuf
import org.joda.time.LocalDateTime

object TimestampDecoder extends BinaryDecoder {
  def decode(buffer: ByteBuf): LocalDateTime = {
    val size = buffer.readUnsignedByte()

    size match {
      case 0 => null
      case 4 => new LocalDateTime()
        .withDate(buffer.readUnsignedShort(), buffer.readUnsignedByte(), buffer.readUnsignedByte())
        .withTime(0, 0, 0, 0)
      case 7 => new LocalDateTime()
        .withDate(buffer.readUnsignedShort(), buffer.readUnsignedByte(), buffer.readUnsignedByte())
        .withTime(buffer.readUnsignedByte(), buffer.readUnsignedByte(), buffer.readUnsignedByte(), 0)
      case 11 => new LocalDateTime()
        .withDate(buffer.readUnsignedShort(), buffer.readUnsignedByte(), buffer.readUnsignedByte())
        .withTime(buffer.readUnsignedByte(), buffer.readUnsignedByte(), buffer.readUnsignedByte(), buffer.readUnsignedInt().toInt / 1000)
    }
  }
}