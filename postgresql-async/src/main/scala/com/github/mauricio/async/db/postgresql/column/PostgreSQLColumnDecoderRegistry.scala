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

package com.github.mauricio.async.db.postgresql.column

import com.github.mauricio.async.db.postgresql.column.ColumnTypes._
import scala.annotation.switch
import com.github.mauricio.async.db.column._

object PostgreSQLColumnDecoderRegistry {
  val Instance = new PostgreSQLColumnDecoderRegistry()
}

class PostgreSQLColumnDecoderRegistry extends ColumnDecoderRegistry {

  def decode(kind: Int, value: String) : Any = decoderFor(kind).decode(value)

  def decoderFor(kind: Int): ColumnDecoder = {
    (kind : @switch) match {
      case Boolean => BooleanEncoderDecoder
      case BooleanArray => new ArrayDecoder(BooleanEncoderDecoder)

      case ColumnTypes.Char => CharEncoderDecoder
      case CharArray => new ArrayDecoder(CharEncoderDecoder)

      case Bigserial => LongEncoderDecoder

      case Smallint => ShortEncoderDecoder
      case SmallintArray => new ArrayDecoder(ShortEncoderDecoder)

      case ColumnTypes.Integer => IntegerEncoderDecoder
      case IntegerArray => new ArrayDecoder(IntegerEncoderDecoder)

      case ColumnTypes.Numeric => BigDecimalEncoderDecoder
      case NumericArray => new ArrayDecoder(BigDecimalEncoderDecoder)

      case Real => FloatEncoderDecoder
      case RealArray => new ArrayDecoder(FloatEncoderDecoder)

      case ColumnTypes.Double => DoubleEncoderDecoder
      case DoubleArray => new ArrayDecoder(DoubleEncoderDecoder)

      case Text => StringEncoderDecoder
      case TextArray => new ArrayDecoder(StringEncoderDecoder)

      case Varchar => StringEncoderDecoder
      case VarcharArray => new ArrayDecoder(StringEncoderDecoder)

      case Bpchar => StringEncoderDecoder
      case BpcharArray => new ArrayDecoder(StringEncoderDecoder)

      case Timestamp => TimestampEncoderDecoder.Instance
      case TimestampArray => new ArrayDecoder(TimestampEncoderDecoder.Instance)

      case TimestampWithTimezone => TimestampWithTimezoneEncoderDecoder
      case TimestampWithTimezoneArray => new ArrayDecoder(TimestampWithTimezoneEncoderDecoder)

      case Date => DateEncoderDecoder
      case DateArray => new ArrayDecoder(DateEncoderDecoder)

      case Time => TimeEncoderDecoder.Instance
      case TimeArray => new ArrayDecoder(TimeEncoderDecoder.Instance)

      case TimeWithTimezone => TimeWithTimezoneEncoderDecoder
      case TimeWithTimezoneArray => new ArrayDecoder(TimeWithTimezoneEncoderDecoder)

      case OIDArray => new ArrayDecoder(StringEncoderDecoder)
      case MoneyArray => new ArrayDecoder(StringEncoderDecoder)
      case NameArray => new ArrayDecoder(StringEncoderDecoder)
      case UUIDArray => new ArrayDecoder(StringEncoderDecoder)
      case XMLArray => new ArrayDecoder(StringEncoderDecoder)

      case _ => StringEncoderDecoder
    }
  }

}
