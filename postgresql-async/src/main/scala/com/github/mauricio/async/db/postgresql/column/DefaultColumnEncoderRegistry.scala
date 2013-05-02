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

import org.joda.time._
import scala.collection.JavaConversions._

object DefaultColumnEncoderRegistry {
  val Instance = new DefaultColumnEncoderRegistry()
}

class DefaultColumnEncoderRegistry extends ColumnEncoderRegistry {

  private val classesSequence = List(
    classOf[Int] -> IntegerEncoderDecoder,
    classOf[java.lang.Integer] -> IntegerEncoderDecoder,

    classOf[java.lang.Short] -> IntegerEncoderDecoder,
    classOf[Short] -> ShortEncoderDecoder,

    classOf[Long] -> LongEncoderDecoder,
    classOf[java.lang.Long] -> LongEncoderDecoder,

    classOf[String] -> StringEncoderDecoder,
    classOf[java.lang.String] -> StringEncoderDecoder,

    classOf[Float] -> FloatEncoderDecoder,
    classOf[java.lang.Float] -> FloatEncoderDecoder,

    classOf[Double] -> DoubleEncoderDecoder,
    classOf[java.lang.Double] -> DoubleEncoderDecoder,

    classOf[BigDecimal] -> BigDecimalEncoderDecoder,
    classOf[java.math.BigDecimal] -> BigDecimalEncoderDecoder,

    classOf[LocalDate] -> DateEncoderDecoder,
    classOf[LocalTime] -> TimeEncoderDecoder.Instance,
    classOf[DateTime] -> TimestampWithTimezoneEncoderDecoder,
    classOf[ReadablePartial] -> TimeEncoderDecoder.Instance,
    classOf[ReadableDateTime] -> TimestampWithTimezoneEncoderDecoder,
    classOf[ReadableInstant] -> DateEncoderDecoder,

    classOf[java.util.Date] -> TimestampWithTimezoneEncoderDecoder,
    classOf[java.sql.Date] -> DateEncoderDecoder,
    classOf[java.sql.Time] -> TimeEncoderDecoder.Instance,
    classOf[java.sql.Timestamp] -> TimestampWithTimezoneEncoderDecoder,
    classOf[java.util.Calendar] -> TimestampWithTimezoneEncoderDecoder,
    classOf[java.util.GregorianCalendar] -> TimestampWithTimezoneEncoderDecoder)

  private val classes: Map[Class[_], ColumnEncoder] = classesSequence.toMap

  def encode(value: Any): String = {

    if (value == null) {
      return null
    }

    val encoder = this.classes.get(value.getClass)

    if (encoder.isDefined) {
      encoder.get.encode(value)
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
            case Some(parent) => parent._2.encode(value)
            case None => value.toString
          }
        }
      }

    }

  }

  def encodeArray(collection: Traversable[_]): String = {
    val builder = new StringBuilder()

    builder.append('{')

    val result = collection.map {
      item =>

        if (item == null) {
          "NULL"
        } else {
          if (this.shouldQuote(item)) {
            "\"" + this.encode(item).replaceAllLiterally("\"", """\"""") + "\""
          } else {
            this.encode(item)
          }
        }

    }.mkString(",")

    builder.append(result)
    builder.append('}')

    builder.toString()
  }

  def shouldQuote(value: Any): Boolean = {
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
      case _ => true
    }
  }

  def kindOf(value: Any): Int = {
    if ( value == null ) {
      0
    } else {
      this.classes.get(value.getClass) match {
        case Some( encoder ) => encoder.kind
        case None => 0
      }
    }
  }
}