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

import com.github.mauricio.async.db.exceptions.BufferNotFullyConsumedException
import com.github.mauricio.async.db.mysql.binary.decoder._
import com.github.mauricio.async.db.mysql.column.ColumnTypes
import com.github.mauricio.async.db.mysql.message.server.ColumnDefinitionMessage
import com.github.mauricio.async.db.util._
import java.nio.charset.Charset
import org.jboss.netty.buffer.ChannelBuffer
import scala.collection.mutable.ArrayBuffer

object BinaryRowDecoder {
  final val log = Log.get[BinaryRowDecoder]
  final val BitMapOffset = 9
}

class BinaryRowDecoder(charset: Charset) {

  import BinaryRowDecoder._

  private final val bigDecimalDecoder = new BigDecimalDecoder(charset)
  private final val stringDecoder = new StringDecoder(charset)

  def decode(buffer: ChannelBuffer, columns: Seq[ColumnDefinitionMessage]): IndexedSeq[Any] = {

    //log.debug("columns are {}", columns)

    //log.debug( "decoding row\n{}", MySQLHelper.dumpAsHex(buffer, buffer.readableBytes()))
    //PrintUtils.printArray("bitmap", buffer)

    val bitMap = BitMap.fromBuffer( columns.size + 7 + 2, buffer  )

    //log.debug("bitmap is {}", bitMap)

    val row = new ArrayBuffer[Any](columns.size)

    bitMap.foreachWithLimit(BitMapOffset, columns.length, {
      case (index, isNull) => {
        if (isNull) {
          row += null
        } else {
          val decoder = decoderFor(columns(index - BitMapOffset).columnType)

          //log.debug(s"${decoder.getClass.getSimpleName} - ${buffer.readableBytes()}")

          row += decoder.decode(buffer)
        }
      }
    })

    //log.debug("values are {}", row)

    if (buffer.readableBytes() != 0) {
      throw new BufferNotFullyConsumedException(buffer)
    }

    row
  }

  def decoderFor(columnType: Int): BinaryDecoder = {
    columnType match {
      case ColumnTypes.FIELD_TYPE_VARCHAR |
           ColumnTypes.FIELD_TYPE_VAR_STRING |
           ColumnTypes.FIELD_TYPE_STRING => this.stringDecoder
      case ColumnTypes.FIELD_TYPE_BLOB |
           ColumnTypes.FIELD_TYPE_LONG_BLOB |
           ColumnTypes.FIELD_TYPE_MEDIUM_BLOB |
           ColumnTypes.FIELD_TYPE_TINY_BLOB => ByteArrayDecoder
      case ColumnTypes.FIELD_TYPE_LONGLONG => LongDecoder
      case ColumnTypes.FIELD_TYPE_LONG | ColumnTypes.FIELD_TYPE_INT24 => IntegerDecoder
      case ColumnTypes.FIELD_TYPE_YEAR | ColumnTypes.FIELD_TYPE_SHORT => ShortDecoder
      case ColumnTypes.FIELD_TYPE_TINY => ByteDecoder
      case ColumnTypes.FIELD_TYPE_DOUBLE => DoubleDecoder
      case ColumnTypes.FIELD_TYPE_FLOAT => FloatDecoder
      case ColumnTypes.FIELD_TYPE_NUMERIC |
           ColumnTypes.FIELD_TYPE_DECIMAL |
           ColumnTypes.FIELD_TYPE_NEW_DECIMAL => this.bigDecimalDecoder
      case ColumnTypes.FIELD_TYPE_DATETIME | ColumnTypes.FIELD_TYPE_TIMESTAMP => TimestampDecoder
      case ColumnTypes.FIELD_TYPE_DATE => DateDecoder
      case ColumnTypes.FIELD_TYPE_TIME => TimeDecoder
    }
  }

}