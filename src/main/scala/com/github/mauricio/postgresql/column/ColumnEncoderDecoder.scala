package com.github.mauricio.postgresql.column

import org.joda.time._
import scala.Some


/**
 * User: Maurício Linhares
 * Date: 3/5/12
 * Time: 9:34 AM
 */

class ColumnDecoderNotFoundException( kind : Int )
  extends IllegalArgumentException( "There is no decoder available for kind %s".format(kind) )

object ColumnEncoderDecoder {

  val Bigserial = 20
  val Char = 18
  val Smallint = 21
  val Integer = 23
  val Numeric = 1700 // Decimal is the same as Numeric on PostgreSQL
  val Real = 700
  val Double = 701
  val Serial = 23
  val Bpchar = 1042
  val Varchar = 1043 // Char is the same as Varchar on PostgreSQL
  val Text = 25
  val Timestamp = 1114
  val TimestampWithTimezone = 1184
  val Date = 1082
  val Time = 1083
  val TimeWithTimezone = 1266
  val Boolean = 16

  private val classes = Map[Class[_], Int](
    classOf[Int] -> Integer,
    classOf[Short] -> Integer,
    classOf[java.lang.Integer] -> Integer,
    classOf[java.lang.Short] -> Integer,

    classOf[Long] -> Bigserial,
    classOf[java.lang.Long] -> Bigserial,

    classOf[String] -> Varchar,
    classOf[java.lang.String] -> Varchar,

    classOf[Float] -> Real,
    classOf[java.lang.Float] -> Real,

    classOf[Double] -> Double,
    classOf[java.lang.Double] -> Double,

    classOf[BigDecimal] -> Numeric,
    classOf[java.math.BigDecimal] -> Numeric,

    classOf[LocalDate] -> Date,
    classOf[LocalTime] -> Time,
    classOf[ReadablePartial] -> Time,
    classOf[ReadableDateTime] -> Timestamp,
    classOf[ReadableInstant] -> Date,
    classOf[DateTime] -> Timestamp,

    classOf[java.util.Date] -> Timestamp,
    classOf[java.sql.Date] -> Date,
    classOf[java.sql.Time] -> Time,
    classOf[java.sql.Timestamp] -> Timestamp,
    classOf[java.util.Calendar] -> Timestamp,
    classOf[java.util.GregorianCalendar] -> Timestamp
  )

  def decoderFor(kind: Int): ColumnEncoderDecoder = {
    kind match {
      case Boolean => BooleanEncoderDecoder
      case Char => StringEncoderDecoder
      case Bigserial => LongEncoderDecoder
      case Smallint => IntegerEncoderDecoder
      case Integer => IntegerEncoderDecoder
      case Text => StringEncoderDecoder
      case Real => FloatEncoderDecoder
      case Double => DoubleEncoderDecoder
      case Varchar => StringEncoderDecoder
      case Bpchar => StringEncoderDecoder
      case Numeric => BigDecimalEncoderDecoder
      case Timestamp => TimestampEncoderDecoder.Instance
      case TimestampWithTimezone => TimestampWithTimezoneEncoderDecoder
      case Date => DateEncoderDecoder
      case Time => TimeEncoderDecoder.Instance
      case TimeWithTimezone => TimeWithTimezoneEncoderDecoder
      case _ => StringEncoderDecoder
    }
  }

  def kindFor( clazz : Class[_] ) : Int = {
    this.classes.get(clazz).getOrElse {
      this.classes.find( entry => entry._1.isAssignableFrom(clazz)  ) match {
        case Some(parent) => parent._2
        case None => 0
      }
    }
  }

  def decode( kind : Int, value : String ) : Any = {
    decoderFor(kind).decode(value)
  }

  def encode( value : Any ) : String = {
    decoderFor( kindFor( value.getClass ) ).encode(value)
  }

}

trait ColumnEncoderDecoder {

  def decode(value: String): Any

  def encode(value: Any) : String = {
    value.toString
  }

}

/*

    public static final int UNSPECIFIED = 0;
    public static final int INT2 = 21;
    public static final int INT2_ARRAY = 1005;
    public static final int INT4 = 23;
    public static final int INT4_ARRAY = 1007;
    public static final int INT8 = 20;
    public static final int INT8_ARRAY = 1016;
    public static final int TEXT = 25;
    public static final int TEXT_ARRAY = 1009;
    public static final int NUMERIC = 1700;
    public static final int NUMERIC_ARRAY = 1231;
    public static final int FLOAT4 = 700;
    public static final int FLOAT4_ARRAY = 1021;
    public static final int FLOAT8 = 701;
    public static final int FLOAT8_ARRAY = 1022;
    public static final int BOOL = 16;
    public static final int BOOL_ARRAY = 1000;
    public static final int DATE = 1082;
    public static final int DATE_ARRAY = 1182;
    public static final int TIME = 1083;
    public static final int TIME_ARRAY = 1183;
    public static final int TIMETZ = 1266;
    public static final int TIMETZ_ARRAY = 1270;
    public static final int TIMESTAMP = 1114;
    public static final int TIMESTAMP_ARRAY = 1115;
    public static final int TIMESTAMPTZ = 1184;
    public static final int TIMESTAMPTZ_ARRAY = 1185;
    public static final int BYTEA = 17;
    public static final int BYTEA_ARRAY = 1001;
    public static final int VARCHAR = 1043;
    public static final int VARCHAR_ARRAY = 1015;
    public static final int OID = 26;
    public static final int OID_ARRAY = 1028;
    public static final int BPCHAR = 1042;
    public static final int BPCHAR_ARRAY = 1014;
    public static final int MONEY = 790;
    public static final int MONEY_ARRAY = 791;
    public static final int NAME = 19;
    public static final int NAME_ARRAY = 1003;
    public static final int BIT = 1560;
    public static final int BIT_ARRAY = 1561;
    public static final int VOID = 2278;
    public static final int INTERVAL = 1186;
    public static final int INTERVAL_ARRAY = 1187;
    public static final int CHAR = 18; // This is not char(N), this is "char" a single byte type.
    public static final int CHAR_ARRAY = 1002;
    public static final int VARBIT = 1562;
    public static final int VARBIT_ARRAY = 1563;
    public static final int UUID = 2950;
    public static final int UUID_ARRAY = 2951;
    public static final int XML = 142;
    public static final int XML_ARRAY = 143;

*/