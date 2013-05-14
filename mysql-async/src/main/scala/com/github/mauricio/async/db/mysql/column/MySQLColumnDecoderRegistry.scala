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

package com.github.mauricio.async.db.mysql.column

import com.github.mauricio.async.db.column._
import scala.annotation.switch
import org.jboss.netty.buffer.ChannelBuffer
import java.nio.charset.Charset

object MySQLColumnDecoderRegistry {
  final val Instance = new MySQLColumnDecoderRegistry()
}

class MySQLColumnDecoderRegistry extends ColumnDecoderRegistry {

  override def decode(kind: Int, value: ChannelBuffer, charset: Charset): Any = {
    decoderFor(kind).decode(value, charset)
  }

  def decoderFor( kind : Int ) : ColumnDecoder = {
    (kind : @switch) match {
      case ColumnTypes.FIELD_TYPE_DATE => DateEncoderDecoder
      case ColumnTypes.FIELD_TYPE_DATETIME => TimestampEncoderDecoder.Instance
      case ColumnTypes.FIELD_TYPE_DECIMAL => BigDecimalEncoderDecoder
      case ColumnTypes.FIELD_TYPE_DOUBLE => DoubleEncoderDecoder
      case ColumnTypes.FIELD_TYPE_FLOAT => FloatEncoderDecoder
      case ColumnTypes.FIELD_TYPE_INT24 => IntegerEncoderDecoder
      case ColumnTypes.FIELD_TYPE_LONG => IntegerEncoderDecoder
      case ColumnTypes.FIELD_TYPE_LONGLONG => LongEncoderDecoder
      case ColumnTypes.FIELD_TYPE_NEW_DECIMAL => BigDecimalEncoderDecoder
      case ColumnTypes.FIELD_TYPE_NUMERIC => BigDecimalEncoderDecoder
      case ColumnTypes.FIELD_TYPE_NEWDATE => DateEncoderDecoder
      case ColumnTypes.FIELD_TYPE_SHORT => ShortEncoderDecoder
      case ColumnTypes.FIELD_TYPE_STRING => StringEncoderDecoder
      case ColumnTypes.FIELD_TYPE_TIME => TimeDecoder
      case ColumnTypes.FIELD_TYPE_TIMESTAMP => TimestampEncoderDecoder.Instance
      case ColumnTypes.FIELD_TYPE_TINY => ByteDecoder
      case ColumnTypes.FIELD_TYPE_VAR_STRING => StringEncoderDecoder
      case ColumnTypes.FIELD_TYPE_VARCHAR => StringEncoderDecoder
      case ColumnTypes.FIELD_TYPE_YEAR => ShortEncoderDecoder
      case _ => StringEncoderDecoder
    }
  }

}