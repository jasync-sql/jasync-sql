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

package com.github.mauricio.async.db.mysql.binary

import _root_.io.netty.buffer.ByteBuf
import com.github.mauricio.async.db.exceptions.BufferNotFullyConsumedException
import com.github.mauricio.async.db.mysql.message.server.ColumnDefinitionMessage
import com.github.mauricio.async.db.util._
import scala.collection.mutable.ArrayBuffer

object BinaryRowDecoder {
  final val log = Log.get[BinaryRowDecoder]
  final val BitMapOffset = 9
}

class BinaryRowDecoder {

  //import BinaryRowDecoder._

  def decode(buffer: ByteBuf, columns: Seq[ColumnDefinitionMessage]): Array[Any] = {

    //log.debug("columns are {} - {}", buffer.readableBytes(), columns)
    //log.debug( "decoding row\n{}", MySQLHelper.dumpAsHex(buffer))
    //PrintUtils.printArray("bitmap", buffer)

    val nullCount = (columns.size + 9) / 8

    val nullBitMask = new Array[Byte](nullCount)
    buffer.readBytes(nullBitMask)

    var nullMaskPos = 0
    var bit = 4

    val row = new ArrayBuffer[Any](columns.size)

    var index = 0

    while (index < columns.size) {

      if ((nullBitMask(nullMaskPos) & bit) != 0) {
        row += null
      } else {

        val column = columns(index)

        //log.debug(s"${decoder.getClass.getSimpleName} - ${buffer.readableBytes()}")
        //log.debug("Column value [{}] - {}", value, column.name)

        row += column.binaryDecoder.decode(buffer)
      }

      bit <<= 1

      if (( bit & 255) == 0) {
        bit = 1
        nullMaskPos += 1
      }

      index += 1
    }

    //log.debug("values are {}", row)

    if (buffer.readableBytes() != 0) {
      throw new BufferNotFullyConsumedException(buffer)
    }

    row.toArray
  }

}