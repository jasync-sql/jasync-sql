package com.github.jasync.sql.db.postgresql.column

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
import org.joda.time.ReadablePartial
import org.joda.time.ReadablePeriod
import java.math.BigDecimal
import java.nio.ByteBuffer


class PostgreSQLColumnEncoderRegistry : ColumnEncoderRegistry {

  companion object {
    val Instance = PostgreSQLColumnEncoderRegistry()
  }

  private val classesSequence_
    get() = listOf(
        Int::class.java to (IntegerEncoderDecoder to ColumnTypes.Numeric),
        java.lang.Integer::class.java to (IntegerEncoderDecoder to ColumnTypes.Numeric),

        java.lang.Short::class.java to (ShortEncoderDecoder to ColumnTypes.Numeric),
        Short::class.java to (ShortEncoderDecoder to ColumnTypes.Numeric),

        Long::class.java to (LongEncoderDecoder to ColumnTypes.Numeric),
        java.lang.Long::class.java to (LongEncoderDecoder to ColumnTypes.Numeric),

        String::class.java to (StringEncoderDecoder to ColumnTypes.Varchar),
        java.lang.String::class.java to (StringEncoderDecoder to ColumnTypes.Varchar),

        Float::class.java to (FloatEncoderDecoder to ColumnTypes.Numeric),
        java.lang.Float::class.java to (FloatEncoderDecoder to ColumnTypes.Numeric),

        Double::class.java to (DoubleEncoderDecoder to ColumnTypes.Numeric),
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
        arrayOf<Byte>()::class.java to (ByteArrayEncoderDecoder to ColumnTypes.ByteA),
        ByteBuffer::class.java to (ByteArrayEncoderDecoder to ColumnTypes.ByteA),
        ByteBuf::class.java to (ByteArrayEncoderDecoder to ColumnTypes.ByteA)
    )

//  private final val classesSequence = (classOf[LocalTime] -> (TimeEncoderDecoder.Instance -> ColumnTypes.Time)) ::
//  (classOf[ReadablePartial] -> (TimeEncoderDecoder.Instance -> ColumnTypes.Time)) ::
//  classesSequence_

  private val classesSequence = listOf(
      LocalTime::class.java to (TimeEncoderDecoder.Instance to ColumnTypes.Time),
      ReadablePartial::class.java to (TimeEncoderDecoder.Instance to ColumnTypes.Time)) + classesSequence_


  private val classes = classesSequence.toMap()

  override fun encode(value: Any?): String? {
    if (value == null) {
      return null
    }

    return encodeValue(value)

  }

  /**
   * Used to encode a value that is not null and not an Option.
   */
  private fun encodeValue(value: Any): String {

    val encoder = this.classes[value.javaClass]

    return if (encoder != null) {
      encoder.first.encode(value)
    } else {
      when (value) {
        is Iterable<*>
        -> encodeArray(value)
        is Array<*>
        -> encodeArray(value.asIterable())
//        p: Product //product is pair tuple etc' currently not supported because not sure if required
//        -> encodeComposite(p)
        else -> {
          val found = this.classesSequence.find { entry -> entry.first.isAssignableFrom(value.javaClass) }
          when {
            found != null -> found.second.first.encode(value)
            else -> value.toString()
          }
        }
      }

    }

  }

//  private fun encodeComposite(p: Product): String {
//    p.productIterator.map { item ->
//      if (item == null || item == None) {
//        "NULL"
//      } else {
//        if (this.shouldQuote(item)) {
//          "\"" + this.encode(item).replaceAllLiterally("\\", """\\""").replaceAllLiterally("\"", """\"""") + "\""
//        } else {
//          this.encode(item)
//        }
//      }
//    }.mkString("(", ",", ")")
//  }
//
  private fun encodeArray(collection: Iterable<*>): String {
    return collection.map { item ->
      if (item == null) {
        "NULL"
      } else {
        if (this.shouldQuote(item)) {
          "\"" + this.encode(item)!!.replace("\\", """\\""").replace("\"", """\"""") + "\""
        } else {
          this.encode(item)
        }
      }
    }.joinToString (",", "{", "}")
  }

  private fun shouldQuote(value: Any): Boolean {
    return when (value) {
      is Number -> false
      is Int      -> false
      is Short      -> false
      is Long      -> false
      is Float      -> false
      is Double      -> false
      is Iterable<*>      -> false
      is Array<*>      -> false
      else -> true
    }
  }

  override fun kindOf(value: Any?): Int {
    return if (value == null) {
      0
    } else {
      when(value) {
        is String -> ColumnTypes.Untyped
        else -> {
          val fromClasses = this.classes[value.javaClass]
           when {
             fromClasses != null -> fromClasses.second
            else -> ColumnTypes.Untyped
          }
        }
      }
    }
  }

}
