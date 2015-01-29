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

import com.github.mauricio.async.db.column._
import com.github.mauricio.async.db.postgresql.column.ColumnTypes._
import scala.annotation.switch
import java.nio.charset.Charset
import io.netty.util.CharsetUtil
import io.netty.buffer.ByteBuf
import com.github.mauricio.async.db.general.ColumnData

object PostgreSQLColumnDecoderRegistry {
  val Instance = new PostgreSQLColumnDecoderRegistry()
}

class PostgreSQLColumnDecoderRegistry( charset : Charset = CharsetUtil.UTF_8 ) extends ColumnDecoderRegistry {

  private final val stringArrayDecoder = new ArrayDecoder(StringEncoderDecoder)
  private final val booleanArrayDecoder = new ArrayDecoder(BooleanEncoderDecoder)
  private final val charArrayDecoder = new ArrayDecoder(CharEncoderDecoder)
  private final val longArrayDecoder = new ArrayDecoder(LongEncoderDecoder)
  private final val shortArrayDecoder = new ArrayDecoder(ShortEncoderDecoder)
  private final val integerArrayDecoder = new ArrayDecoder(IntegerEncoderDecoder)
  private final val bigDecimalArrayDecoder = new ArrayDecoder(BigDecimalEncoderDecoder)
  private final val floatArrayDecoder = new ArrayDecoder(FloatEncoderDecoder)
  private final val doubleArrayDecoder = new ArrayDecoder(DoubleEncoderDecoder)
  private final val timestampArrayDecoder = new ArrayDecoder(PostgreSQLTimestampEncoderDecoder)
  private final val timestampWithTimezoneArrayDecoder = new ArrayDecoder(PostgreSQLTimestampEncoderDecoder)
  private final val dateArrayDecoder =  new ArrayDecoder(DateEncoderDecoder)
  private final val timeArrayDecoder = new ArrayDecoder(TimeEncoderDecoder.Instance)
  private final val timeWithTimestampArrayDecoder = new ArrayDecoder(TimeWithTimezoneEncoderDecoder)
  private final val intervalArrayDecoder = new ArrayDecoder(PostgreSQLIntervalEncoderDecoder)
  private final val uuidArrayDecoder = new ArrayDecoder(UUIDEncoderDecoder)

  override def decode(kind: ColumnData, value: ByteBuf, charset: Charset): Any = {
    decoderFor(kind.dataType).decode(kind, value, charset)
  }

  def decoderFor(kind: Int): ColumnDecoder = {
    (kind : @switch) match {
      case Boolean => BooleanEncoderDecoder
      case BooleanArray => this.booleanArrayDecoder

      case ColumnTypes.Char => CharEncoderDecoder
      case CharArray => this.charArrayDecoder

      case Bigserial => LongEncoderDecoder
      case BigserialArray => this.longArrayDecoder

      case Smallint => ShortEncoderDecoder
      case SmallintArray => this.shortArrayDecoder

      case ColumnTypes.Integer => IntegerEncoderDecoder
      case IntegerArray => this.integerArrayDecoder

      case OID => LongEncoderDecoder
      case OIDArray => this.longArrayDecoder

      case ColumnTypes.Numeric => BigDecimalEncoderDecoder
      case NumericArray => this.bigDecimalArrayDecoder

      case Real => FloatEncoderDecoder
      case RealArray => this.floatArrayDecoder

      case ColumnTypes.Double => DoubleEncoderDecoder
      case DoubleArray => this.doubleArrayDecoder

      case Text => StringEncoderDecoder
      case TextArray => this.stringArrayDecoder

      case Varchar => StringEncoderDecoder
      case VarcharArray => this.stringArrayDecoder

      case Bpchar => StringEncoderDecoder
      case BpcharArray => this.stringArrayDecoder

      case Timestamp => PostgreSQLTimestampEncoderDecoder
      case TimestampArray => this.timestampArrayDecoder

      case TimestampWithTimezone => PostgreSQLTimestampEncoderDecoder
      case TimestampWithTimezoneArray => this.timestampWithTimezoneArrayDecoder

      case Date => DateEncoderDecoder
      case DateArray => this.dateArrayDecoder

      case Time => TimeEncoderDecoder.Instance
      case TimeArray => this.timeArrayDecoder

      case TimeWithTimezone => TimeWithTimezoneEncoderDecoder
      case TimeWithTimezoneArray => this.timeWithTimestampArrayDecoder

      case Interval => PostgreSQLIntervalEncoderDecoder
      case IntervalArray => this.intervalArrayDecoder

      case MoneyArray => this.stringArrayDecoder
      case NameArray => this.stringArrayDecoder
      case UUID => UUIDEncoderDecoder
      case UUIDArray => this.uuidArrayDecoder
      case XMLArray => this.stringArrayDecoder
      case ByteA => ByteArrayEncoderDecoder

      case _ => StringEncoderDecoder
    }
  }

}
