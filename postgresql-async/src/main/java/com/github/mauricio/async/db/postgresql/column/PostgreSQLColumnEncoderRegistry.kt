package com.github.mauricio.async.db.postgresql.column

import com.github.jasync.sql.db.column.BigDecimalEncoderDecoder
import com.github.jasync.sql.db.column.ColumnEncoderRegistry
import com.github.jasync.sql.db.column.DateEncoderDecoder
import com.github.jasync.sql.db.column.DoubleEncoderDecoder
import com.github.jasync.sql.db.column.FloatEncoderDecoder
import com.github.jasync.sql.db.column.InetAddressEncoderDecoder
import com.github.jasync.sql.db.column.IntegerEncoderDecoder
import com.github.jasync.sql.db.column.LongEncoderDecoder
import com.github.jasync.sql.db.column.SQLTimeEncoder
import com.github.jasync.sql.db.column.ShortEncoderDecoder
import com.github.jasync.sql.db.column.StringEncoderDecoder
import com.github.jasync.sql.db.column.TimeEncoderDecoder
import com.github.jasync.sql.db.column.TimestampEncoderDecoder
import com.github.jasync.sql.db.column.TimestampWithTimezoneEncoderDecoder
import com.github.jasync.sql.db.column.UUIDEncoderDecoder
import io.netty.buffer.ByteBuf
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime
import org.joda.time.LocalTime
import org.joda.time.ReadableDateTime
import org.joda.time.ReadableDuration
import org.joda.time.ReadableInstant
import org.joda.time.ReadablePeriod
import java.math.BigDecimal
import java.nio.ByteBuffer


class PostgreSQLColumnEncoderRegistry : ColumnEncoderRegistry {

  companion object {
    val Instance = PostgreSQLColumnEncoderRegistry()
  }

  private val classesSequence_
    get() = listOf(
        Int.javaClass to (IntegerEncoderDecoder to ColumnTypes.Numeric),
        java.lang.Integer::class.java to (IntegerEncoderDecoder to ColumnTypes.Numeric),

        java.lang.Short::class.java to (ShortEncoderDecoder to ColumnTypes.Numeric),
        Short.javaClass to (ShortEncoderDecoder to ColumnTypes.Numeric),

        Long.javaClass to (LongEncoderDecoder to ColumnTypes.Numeric),
        java.lang.Long::class.java to (LongEncoderDecoder to ColumnTypes.Numeric),

        String.javaClass to (StringEncoderDecoder to ColumnTypes.Varchar),
        java.lang.String::class.java to (StringEncoderDecoder to ColumnTypes.Varchar),

        Float.javaClass to (FloatEncoderDecoder to ColumnTypes.Numeric),
        java.lang.Float::class.java to (FloatEncoderDecoder to ColumnTypes.Numeric),

        Double.javaClass to (DoubleEncoderDecoder to ColumnTypes.Numeric),
        java.lang.Double::class.java to (DoubleEncoderDecoder to ColumnTypes.Numeric),

        BigDecimal::class.java to (BigDecimalEncoderDecoder to ColumnTypes.Numeric),

        java.net.InetAddress::class.java to (InetAddressEncoderDecoder to ColumnTypes.Inet),

        java.util.UUID::class.java to (UUIDEncoderDecoder to ColumnTypes.UUID),

        LocalDate::class.java to (DateEncoderDecoder to ColumnTypes.Date),
        LocalDateTime::class.java to (TimestampEncoderDecoder.Instance to ColumnTypes.Timestamp),
        DateTime::class.java to (TimestampWithTimezoneEncoderDecoder to ColumnTypes.TimestampWithTimezone),
        ReadableDateTime::class.java to (TimestampWithTimezoneEncoderDecoder to ColumnTypes.TimestampWithTimezone),
        ReadableInstant::class.java to (DateEncoderDecoder to ColumnTypes.Date),

        ReadablePeriod::class.java to (PostgreSQLIntervalEncoderDecoder to ColumnTypes.Interval),
        ReadableDuration::class.java to (PostgreSQLIntervalEncoderDecoder to ColumnTypes.Interval),

        java.util.Date::class.java to (TimestampWithTimezoneEncoderDecoder to ColumnTypes.TimestampWithTimezone),
        java.sql.Date::class.java to (DateEncoderDecoder to ColumnTypes.Date),
        java.sql.Time::class.java to (SQLTimeEncoder to ColumnTypes.Time),
        java.sql.Timestamp::class.java to (TimestampWithTimezoneEncoderDecoder to ColumnTypes.TimestampWithTimezone),
        java.util.Calendar::class.java to (TimestampWithTimezoneEncoderDecoder to ColumnTypes.TimestampWithTimezone),
        java.util.GregorianCalendar::class.java to (TimestampWithTimezoneEncoderDecoder to ColumnTypes.TimestampWithTimezone),
        arrayOf<Byte>().javaClass to (ByteArrayEncoderDecoder to ColumnTypes.ByteA),
        ByteBuffer::class.java to (ByteArrayEncoderDecoder to ColumnTypes.ByteA),
        ByteBuf::class.java to (ByteArrayEncoderDecoder to ColumnTypes.ByteA)
    )

  private val classesSequence = LocalTime::class.java to (TimeEncoderDecoder.Instance -> ColumnTypes.Time)
  (ReadablePartial.javaClass -> (TimeEncoderDecoder.Instance -> ColumnTypes.Time))
  classesSequence_

  private val classes = classesSequence.toMap

  override fun encode(value: Any): String {
    if (value == null) {
      return null
    }

     when(value) {
      Some(v) -> encode(v)
      None -> null
      else -> encodeValue(value)
    }

  }

  /**
   * Used to encode a value that is not null and not an Option.
   */
  private fun encodeValue(value: Any): String {

    val encoder = this.classes.get(value.getClass)

    if (encoder.isDefined) {
      encoder.get._1.encode(value)
    } else {
      value when {
        i: java.lang.Iterable<_>
        -> encodeArray(i.toIterable)
        i: Traversable<_>
        -> encodeArray(i)
        i: Array<_>
        -> encodeArray(i.toIterable)
        p: Product
        -> encodeComposite(p)
        else -> {
          this.classesSequence.find(entry -> entry._1.isAssignableFrom(value.getClass)) when {
            Some(parent) -> parent._2._1.encode(value)
            None -> value.toString
          }
        }
      }

    }

  }

  private fun encodeComposite(p: Product): String {
    p.productIterator.map { item ->
      if (item == null || item == None) {
        "NULL"
      } else {
        if (this.shouldQuote(item)) {
          "\"" + this.encode(item).replaceAllLiterally("\\", """\\""").replaceAllLiterally("\"", """\"""") + "\""
        } else {
          this.encode(item)
        }
      }
    }.mkString("(", ",", ")")
  }

  private fun encodeArray(collection: Traversable<_>): String {
    collection.map { item ->
      if (item == null || item == None) {
        "NULL"
      } else {
        if (this.shouldQuote(item)) {
          "\"" + this.encode(item).replaceAllLiterally("\\", """\\""").replaceAllLiterally("\"", """\"""") + "\""
        } else {
          this.encode(item)
        }
      }
    }.mkString("{", ",", "}")
  }

  private fun shouldQuote(value: Any): Boolean {
    value when {
      n: java.lang.Number
      -> false
      n: Int
      -> false
      n: Short
      -> false
      n: Long
      -> false
      n: Float
      -> false
      n: Double
      -> false
      n: java.lang.Iterable<_>
      -> false
      n: Traversable<_>
      -> false
      n: Array<_>
      -> false
      Some(v) -> shouldQuote(v)
      else -> true
    }
  }

  override fun kindOf(value: Any): Int {
    if (value == null || value == None) {
      0
    } else {
      value when {
        Some(v) -> kindOf(v)
        v : String
        -> ColumnTypes.Untyped
        else -> {
          this.classes.get(value.getClass) when {
            Some(entry) -> entry._2
            None -> ColumnTypes.Untyped
          }
        }
      }
    }
  }

}