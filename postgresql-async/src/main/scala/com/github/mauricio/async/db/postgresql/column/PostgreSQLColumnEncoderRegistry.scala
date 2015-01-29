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

import java.nio.ByteBuffer

import com.github.mauricio.async.db.column._
import io.netty.buffer.ByteBuf
import org.joda.time._

import scala.collection.JavaConversions._

object PostgreSQLColumnEncoderRegistry {
  val Instance = new PostgreSQLColumnEncoderRegistry()
}

class PostgreSQLColumnEncoderRegistry extends ColumnEncoderRegistry {

  private val classesSequence_ : List[(Class[_], (ColumnEncoder, Int))] = List(
    classOf[Int] -> (IntegerEncoderDecoder -> ColumnTypes.Integer),
    classOf[java.lang.Integer] -> (IntegerEncoderDecoder -> ColumnTypes.Integer),

    classOf[java.lang.Short] -> (ShortEncoderDecoder -> ColumnTypes.Smallint),
    classOf[Short] -> (ShortEncoderDecoder -> ColumnTypes.Smallint),

    classOf[Long] -> (LongEncoderDecoder -> ColumnTypes.Bigserial),
    classOf[java.lang.Long] -> (LongEncoderDecoder -> ColumnTypes.Bigserial),

    classOf[String] -> (StringEncoderDecoder -> ColumnTypes.Varchar),
    classOf[java.lang.String] -> (StringEncoderDecoder -> ColumnTypes.Varchar),

    classOf[Float] -> (FloatEncoderDecoder -> ColumnTypes.Real),
    classOf[java.lang.Float] -> (FloatEncoderDecoder -> ColumnTypes.Real),

    classOf[Double] -> (DoubleEncoderDecoder -> ColumnTypes.Double),
    classOf[java.lang.Double] -> (DoubleEncoderDecoder -> ColumnTypes.Double),

    classOf[BigDecimal] -> (BigDecimalEncoderDecoder -> ColumnTypes.Numeric),
    classOf[java.math.BigDecimal] -> (BigDecimalEncoderDecoder -> ColumnTypes.Numeric),

    classOf[java.util.UUID] -> (UUIDEncoderDecoder -> ColumnTypes.UUID),

    classOf[LocalDate] -> ( DateEncoderDecoder -> ColumnTypes.Date ),
    classOf[LocalDateTime] -> (TimestampEncoderDecoder.Instance -> ColumnTypes.Timestamp),
    classOf[DateTime] -> (TimestampWithTimezoneEncoderDecoder -> ColumnTypes.TimestampWithTimezone),
    classOf[ReadableDateTime] -> (TimestampWithTimezoneEncoderDecoder -> ColumnTypes.TimestampWithTimezone),
    classOf[ReadableInstant] -> (DateEncoderDecoder -> ColumnTypes.Date),

    classOf[ReadablePeriod] -> (PostgreSQLIntervalEncoderDecoder -> ColumnTypes.Interval),
    classOf[ReadableDuration] -> (PostgreSQLIntervalEncoderDecoder -> ColumnTypes.Interval),

    classOf[java.util.Date] -> (TimestampWithTimezoneEncoderDecoder -> ColumnTypes.TimestampWithTimezone),
    classOf[java.sql.Date] -> ( DateEncoderDecoder -> ColumnTypes.Date ),
    classOf[java.sql.Time] -> ( SQLTimeEncoder -> ColumnTypes.Time ),
    classOf[java.sql.Timestamp] -> (TimestampWithTimezoneEncoderDecoder -> ColumnTypes.TimestampWithTimezone),
    classOf[java.util.Calendar] -> (TimestampWithTimezoneEncoderDecoder -> ColumnTypes.TimestampWithTimezone),
    classOf[java.util.GregorianCalendar] -> (TimestampWithTimezoneEncoderDecoder -> ColumnTypes.TimestampWithTimezone),
    classOf[Array[Byte]] -> ( ByteArrayEncoderDecoder -> ColumnTypes.ByteA ),
    classOf[ByteBuffer] -> ( ByteArrayEncoderDecoder -> ColumnTypes.ByteA ),
    classOf[ByteBuf] -> ( ByteArrayEncoderDecoder -> ColumnTypes.ByteA )
  )

  private final val classesSequence = (classOf[LocalTime] -> (TimeEncoderDecoder.Instance -> ColumnTypes.Time)) ::
    (classOf[ReadablePartial] -> (TimeEncoderDecoder.Instance -> ColumnTypes.Time)) ::
    classesSequence_

  private final val classes = classesSequence.toMap

  override def encode(value: Any): String = {

    if (value == null) {
      return null
    }

    value match {
      case Some(v) => encode(v)
      case None => null
      case _ => encodeValue(value)
    }

  }

  /**
   * Used to encode a value that is not null and not an Option.
   */
  private def encodeValue(value: Any): String = {

    val encoder = this.classes.get(value.getClass)

    if (encoder.isDefined) {
      encoder.get._1.encode(value)
    } else {

      val view: Option[Traversable[Any]] = value match {
        case i: java.lang.Iterable[_] => Some(i.toIterable)
        case i: Traversable[_] => Some(i)
        case i: Array[_] => Some(i.toIterable)
        case _ => None
      }

      view match {
        case Some(collection) => encodeArray(collection)
        case None => {
          this.classesSequence.find(entry => entry._1.isAssignableFrom(value.getClass)) match {
            case Some(parent) => parent._2._1.encode(value)
            case None => value.toString
          }
        }
      }

    }

  }

  private def encodeArray(collection: Traversable[_]): String = {
    val builder = new StringBuilder()

    builder.append('{')

    val result = collection.map {
      item =>

        if (item == null || item == None) {
          "NULL"
        } else {
          if (this.shouldQuote(item)) {
            "\"" + this.encode(item).replaceAllLiterally("\\", """\\""").replaceAllLiterally("\"", """\"""") + "\""
          } else {
            this.encode(item)
          }
        }

    }.mkString(",")

    builder.append(result)
    builder.append('}')

    builder.toString()
  }

  private def shouldQuote(value: Any): Boolean = {
    value match {
      case n: java.lang.Number => false
      case n: Int => false
      case n: Short => false
      case n: Long => false
      case n: Float => false
      case n: Double => false
      case n: java.lang.Iterable[_] => false
      case n: Traversable[_] => false
      case n: Array[_] => false
      case Some(v) => shouldQuote(v)
      case _ => true
    }
  }

  override def kindOf(value: Any): Int = {
    if ( value == null || value == None ) {
      0
    } else {
      value match {
        case Some(v) => kindOf(v)
        case v : String => ColumnTypes.Untyped
        case _ => {
          this.classes.get(value.getClass) match {
            case Some( entry ) => entry._2
            case None => ColumnTypes.Untyped
          }
        }
      }
    }
  }

}
