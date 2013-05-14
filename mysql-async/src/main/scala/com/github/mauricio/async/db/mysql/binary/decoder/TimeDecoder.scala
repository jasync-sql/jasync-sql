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

import org.jboss.netty.buffer.ChannelBuffer
import scala.concurrent.duration._

object TimeDecoder extends BinaryDecoder {
  def decode(buffer: ChannelBuffer): Duration = {

    buffer.readUnsignedByte() match {
      case 0 => 0.seconds
      case 8 => {

        val isNegative = buffer.readUnsignedByte() == 1

        val duration = buffer.readUnsignedInt().days +
          buffer.readUnsignedByte().hours +
          buffer.readUnsignedByte().minutes +
          buffer.readUnsignedByte().seconds

        if ( isNegative ) {
          duration.neg()
        } else {
          duration
        }

      }
      case 12 => {

        val isNegative = buffer.readUnsignedByte() == 1

        val duration = buffer.readUnsignedInt().days +
          buffer.readUnsignedByte().hours +
          buffer.readUnsignedByte().minutes +
          buffer.readUnsignedByte().seconds +
          buffer.readUnsignedInt().micros

        if ( isNegative ) {
          duration.neg()
        } else {
          duration
        }

      }
    }

  }
}
