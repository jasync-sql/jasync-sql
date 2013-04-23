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

package com.github.mauricio.async.db.postgresql.parsers

import com.github.mauricio.async.db.postgresql.messages.backend.{DataRowMessage, Message}
import org.jboss.netty.buffer.ChannelBuffer

object DataRowParser extends Decoder {

  def parseMessage(buffer: ChannelBuffer): Message = {

    val row = new Array[ChannelBuffer](buffer.readShort())

    0.until(row.length).foreach {
      column =>
        val length = buffer.readInt()

        row(column) = if (length == -1) {
          null
        } else {
          val slice = buffer.slice(buffer.readerIndex(), length)
          buffer.readerIndex(buffer.readerIndex() + length)
          slice
        }
    }

    new DataRowMessage(row)
  }

}
