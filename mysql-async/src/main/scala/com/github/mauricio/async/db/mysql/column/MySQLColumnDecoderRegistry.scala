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
import com.github.mauricio.async.db.mysql.message.server.ColumnDefinitionMessage
import com.github.mauricio.async.db.mysql.util.CharsetMapper

object MySQLColumnDecoderRegistry {
  final val Instance = new MySQLColumnDecoderRegistry()
}

class MySQLColumnDecoderRegistry {

  def decode( columnType : ColumnDefinitionMessage , value: ChannelBuffer, charset: Charset): Any = {

    val kind = if ( columnType.dataType == ColumnTypes.FIELD_TYPE_BLOB &&
      columnType.characterSet != CharsetMapper.Binary ) {
      ColumnTypes.FIELD_TYPE_STRING
    } else {
      columnType.dataType
    }

    decoderFor(kind).decode(value, charset)
  }

  def decoderFor( kind : Int ) : ColumnDecoder = {
    (kind : @switch) match {
      case ColumnTypes.FIELD_TYPE_DATE => DateEncoderDecoder
      case ColumnTypes.FIELD_TYPE_DATETIME |
           ColumnTypes.FIELD_TYPE_TIMESTAMP => LocalDateTimeEncoderDecoder
      case ColumnTypes.FIELD_TYPE_DECIMAL |
           ColumnTypes.FIELD_TYPE_NEW_DECIMAL |
           ColumnTypes.FIELD_TYPE_NUMERIC => BigDecimalEncoderDecoder
      case ColumnTypes.FIELD_TYPE_DOUBLE => DoubleEncoderDecoder
      case ColumnTypes.FIELD_TYPE_FLOAT => FloatEncoderDecoder
      case ColumnTypes.FIELD_TYPE_INT24 => IntegerEncoderDecoder
      case ColumnTypes.FIELD_TYPE_LONG => IntegerEncoderDecoder
      case ColumnTypes.FIELD_TYPE_LONGLONG => LongEncoderDecoder
      case ColumnTypes.FIELD_TYPE_NEWDATE => DateEncoderDecoder
      case ColumnTypes.FIELD_TYPE_SHORT => ShortEncoderDecoder
      case ColumnTypes.FIELD_TYPE_TIME => TimeDecoder
      case ColumnTypes.FIELD_TYPE_TINY => ByteDecoder
      case ColumnTypes.FIELD_TYPE_VAR_STRING |
           ColumnTypes.FIELD_TYPE_VARCHAR |
           ColumnTypes.FIELD_TYPE_STRING |
           ColumnTypes.FIELD_TYPE_ENUM => StringEncoderDecoder
      case ColumnTypes.FIELD_TYPE_YEAR => ShortEncoderDecoder
      case ColumnTypes.FIELD_TYPE_BLOB => ByteArrayColumnDecoder
      case _ => StringEncoderDecoder
    }
  }

}