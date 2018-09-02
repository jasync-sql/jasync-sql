package com.github.mauricio.async.db.postgresql.column

import com.github.jasync.sql.db.column.BigDecimalEncoderDecoder
import com.github.jasync.sql.db.column.ColumnDecoder
import com.github.jasync.sql.db.column.ColumnDecoderRegistry
import com.github.jasync.sql.db.column.DateEncoderDecoder
import com.github.jasync.sql.db.column.DoubleEncoderDecoder
import com.github.jasync.sql.db.column.FloatEncoderDecoder
import com.github.jasync.sql.db.column.InetAddressEncoderDecoder
import com.github.jasync.sql.db.column.IntegerEncoderDecoder
import com.github.jasync.sql.db.column.LongEncoderDecoder
import com.github.jasync.sql.db.column.ShortEncoderDecoder
import com.github.jasync.sql.db.column.StringEncoderDecoder
import com.github.jasync.sql.db.column.TimeEncoderDecoder
import com.github.jasync.sql.db.column.TimeWithTimezoneEncoderDecoder
import com.github.jasync.sql.db.column.UUIDEncoderDecoder
import com.github.jasync.sql.db.general.ColumnData
import io.netty.buffer.ByteBuf
import io.netty.util.CharsetUtil
import java.nio.charset.Charset


class PostgreSQLColumnDecoderRegistry(val charset: Charset = CharsetUtil.UTF_8) : ColumnDecoderRegistry {
  companion object {
    val Instance = PostgreSQLColumnDecoderRegistry()
  }

  private val stringArrayDecoder = ArrayDecoder(StringEncoderDecoder)
  private val booleanArrayDecoder = ArrayDecoder(BooleanEncoderDecoder)
  private val charArrayDecoder = ArrayDecoder(CharEncoderDecoder)
  private val longArrayDecoder = ArrayDecoder(LongEncoderDecoder)
  private val shortArrayDecoder = ArrayDecoder(ShortEncoderDecoder)
  private val integerArrayDecoder = ArrayDecoder(IntegerEncoderDecoder)
  private val bigDecimalArrayDecoder = ArrayDecoder(BigDecimalEncoderDecoder)
  private val floatArrayDecoder = ArrayDecoder(FloatEncoderDecoder)
  private val doubleArrayDecoder = ArrayDecoder(DoubleEncoderDecoder)
  private val timestampArrayDecoder = ArrayDecoder(PostgreSQLTimestampEncoderDecoder)
  private val timestampWithTimezoneArrayDecoder = ArrayDecoder(PostgreSQLTimestampEncoderDecoder)
  private val dateArrayDecoder = ArrayDecoder(DateEncoderDecoder)
  private val timeArrayDecoder = ArrayDecoder(TimeEncoderDecoder.Instance)
  private val timeWithTimestampArrayDecoder = ArrayDecoder(TimeWithTimezoneEncoderDecoder)
  private val intervalArrayDecoder = ArrayDecoder(PostgreSQLIntervalEncoderDecoder)
  private val uuidArrayDecoder = ArrayDecoder(UUIDEncoderDecoder)
  private val inetAddressArrayDecoder = ArrayDecoder(InetAddressEncoderDecoder)

  override fun decode(kind: ColumnData, value: ByteBuf, charset: Charset): Any {
    return decoderFor(kind.dataType()).decode(kind, value, charset)!!
  }

  private fun decoderFor(kind: Int): ColumnDecoder {
    (kind : @switch) when() {

      Boolean -> BooleanEncoderDecoder
      BooleanArray -> this.booleanArrayDecoder

      ColumnTypes.Char -> CharEncoderDecoder
      CharArray -> this.charArrayDecoder

      Bigserial -> LongEncoderDecoder
      BigserialArray -> this.longArrayDecoder

      Smallint -> ShortEncoderDecoder
      SmallintArray -> this.shortArrayDecoder

      ColumnTypes.Integer -> IntegerEncoderDecoder
      IntegerArray -> this.integerArrayDecoder

      OID -> LongEncoderDecoder
      OIDArray -> this.longArrayDecoder

      ColumnTypes.Numeric -> BigDecimalEncoderDecoder
      NumericArray -> this.bigDecimalArrayDecoder

      Real -> FloatEncoderDecoder
      RealArray -> this.floatArrayDecoder

      ColumnTypes.Double -> DoubleEncoderDecoder
      DoubleArray -> this.doubleArrayDecoder

      Text -> StringEncoderDecoder
      TextArray -> this.stringArrayDecoder

      Varchar -> StringEncoderDecoder
      VarcharArray -> this.stringArrayDecoder

      Bpchar -> StringEncoderDecoder
      BpcharArray -> this.stringArrayDecoder

      Timestamp -> PostgreSQLTimestampEncoderDecoder
      TimestampArray -> this.timestampArrayDecoder

      TimestampWithTimezone -> PostgreSQLTimestampEncoderDecoder
      TimestampWithTimezoneArray -> this.timestampWithTimezoneArrayDecoder

      Date -> DateEncoderDecoder
      DateArray -> this.dateArrayDecoder

      Time -> TimeEncoderDecoder.Instance
      TimeArray -> this.timeArrayDecoder

      TimeWithTimezone -> TimeWithTimezoneEncoderDecoder
      TimeWithTimezoneArray -> this.timeWithTimestampArrayDecoder

      Interval -> PostgreSQLIntervalEncoderDecoder
      IntervalArray -> this.intervalArrayDecoder

      MoneyArray -> this.stringArrayDecoder
      NameArray -> this.stringArrayDecoder
      UUID -> UUIDEncoderDecoder
      UUIDArray -> this.uuidArrayDecoder
      XMLArray -> this.stringArrayDecoder
      ByteA -> ByteArrayEncoderDecoder

      Inet -> InetAddressEncoderDecoder
      InetArray -> this.inetAddressArrayDecoder

      else -> StringEncoderDecoder
    }
  }

}