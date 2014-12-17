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

import java.nio.ByteBuffer
import java.nio.charset.Charset

import com.github.mauricio.async.db.mysql.binary.encoder._
import com.github.mauricio.async.db.util._
import io.netty.buffer.ByteBuf
import org.joda.time._

object BinaryRowEncoder {
  final val log = Log.get[BinaryRowEncoder]
}

class BinaryRowEncoder( charset : Charset ) {

  private final val stringEncoder = new StringEncoder(charset)
  private final val encoders = Map[Class[_],BinaryEncoder](
    classOf[String] -> this.stringEncoder,
    classOf[BigInt] -> this.stringEncoder,
    classOf[BigDecimal] -> this.stringEncoder,
    classOf[java.math.BigDecimal] -> this.stringEncoder,
    classOf[java.math.BigInteger] -> this.stringEncoder,
    classOf[Byte] -> ByteEncoder,
    classOf[java.lang.Byte] -> ByteEncoder,
    classOf[Short] -> ShortEncoder,
    classOf[java.lang.Short] -> ShortEncoder,
    classOf[Int] -> IntegerEncoder,
    classOf[java.lang.Integer] -> IntegerEncoder,
    classOf[Long] -> LongEncoder,
    classOf[java.lang.Long] -> LongEncoder,
    classOf[Float] -> FloatEncoder,
    classOf[java.lang.Float] -> FloatEncoder,
    classOf[Double] -> DoubleEncoder,
    classOf[java.lang.Double] -> DoubleEncoder,
    classOf[LocalDateTime] -> LocalDateTimeEncoder,
    classOf[DateTime] -> DateTimeEncoder,
    classOf[LocalDate] -> LocalDateEncoder,
    classOf[java.util.Date] -> JavaDateEncoder,
    classOf[java.sql.Timestamp] -> SQLTimestampEncoder,
    classOf[java.sql.Date] -> SQLDateEncoder,
    classOf[java.sql.Time] -> SQLTimeEncoder,
    classOf[scala.concurrent.duration.FiniteDuration] -> DurationEncoder,
    classOf[Array[Byte]] -> ByteArrayEncoder,
    classOf[Boolean] -> BooleanEncoder,
    classOf[java.lang.Boolean] -> BooleanEncoder
  )

  def encoderFor( v : Any ) : BinaryEncoder = {

    this.encoders.get(v.getClass) match {
      case Some(encoder) => encoder
      case None => {
        v match {
          case v : CharSequence => this.stringEncoder
          case v : BigInt => this.stringEncoder
          case v : java.math.BigInteger => this.stringEncoder
          case v : BigDecimal => this.stringEncoder
          case v : java.math.BigDecimal => this.stringEncoder
          case v : ReadableDateTime => DateTimeEncoder
          case v : ReadableInstant => ReadableInstantEncoder
          case v : LocalDateTime => LocalDateTimeEncoder
          case v : java.sql.Timestamp => SQLTimestampEncoder
          case v : java.sql.Date => SQLDateEncoder
          case v : java.util.Calendar => CalendarEncoder
          case v : LocalDate => LocalDateEncoder
          case v : LocalTime => LocalTimeEncoder
          case v : java.sql.Time => SQLTimeEncoder
          case v : scala.concurrent.duration.Duration => DurationEncoder
          case v : java.util.Date => JavaDateEncoder
          case v : ByteBuffer => ByteBufferEncoder
          case v : ByteBuf => ByteBufEncoder
        }
      }
    }

  }

}
