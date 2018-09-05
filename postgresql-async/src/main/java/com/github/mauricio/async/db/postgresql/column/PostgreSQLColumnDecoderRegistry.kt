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
        return when (kind) {

            ColumnTypes.Boolean -> BooleanEncoderDecoder
            ColumnTypes.BooleanArray -> this.booleanArrayDecoder

            ColumnTypes.Char -> CharEncoderDecoder
            ColumnTypes.CharArray -> this.charArrayDecoder

            ColumnTypes.Bigserial -> LongEncoderDecoder
            ColumnTypes.BigserialArray -> this.longArrayDecoder

            ColumnTypes.Smallint -> ShortEncoderDecoder
            ColumnTypes.SmallintArray -> this.shortArrayDecoder

            ColumnTypes.Integer -> IntegerEncoderDecoder
            ColumnTypes.IntegerArray -> this.integerArrayDecoder

            ColumnTypes.OID -> LongEncoderDecoder
            ColumnTypes.OIDArray -> this.longArrayDecoder

            ColumnTypes.Numeric -> BigDecimalEncoderDecoder
            ColumnTypes.NumericArray -> this.bigDecimalArrayDecoder

            ColumnTypes.Real -> FloatEncoderDecoder
            ColumnTypes.RealArray -> this.floatArrayDecoder

            ColumnTypes.Double -> DoubleEncoderDecoder
            ColumnTypes.DoubleArray -> this.doubleArrayDecoder

            ColumnTypes.Text -> StringEncoderDecoder
            ColumnTypes.TextArray -> this.stringArrayDecoder

            ColumnTypes.Varchar -> StringEncoderDecoder
            ColumnTypes.VarcharArray -> this.stringArrayDecoder

            ColumnTypes.Bpchar -> StringEncoderDecoder
            ColumnTypes.BpcharArray -> this.stringArrayDecoder

            ColumnTypes.Timestamp -> PostgreSQLTimestampEncoderDecoder
            ColumnTypes.TimestampArray -> this.timestampArrayDecoder

            ColumnTypes.TimestampWithTimezone -> PostgreSQLTimestampEncoderDecoder
            ColumnTypes.TimestampWithTimezoneArray -> this.timestampWithTimezoneArrayDecoder

            ColumnTypes.Date -> DateEncoderDecoder
            ColumnTypes.DateArray -> this.dateArrayDecoder

            ColumnTypes.Time -> TimeEncoderDecoder.Instance
            ColumnTypes.TimeArray -> this.timeArrayDecoder

            ColumnTypes.TimeWithTimezone -> TimeWithTimezoneEncoderDecoder
            ColumnTypes.TimeWithTimezoneArray -> this.timeWithTimestampArrayDecoder

            ColumnTypes.Interval -> PostgreSQLIntervalEncoderDecoder
            ColumnTypes.IntervalArray -> this.intervalArrayDecoder

            ColumnTypes.MoneyArray -> this.stringArrayDecoder
            ColumnTypes.NameArray -> this.stringArrayDecoder
            ColumnTypes.UUID -> UUIDEncoderDecoder
            ColumnTypes.UUIDArray -> this.uuidArrayDecoder
            ColumnTypes.XMLArray -> this.stringArrayDecoder
            ColumnTypes.ByteA -> ByteArrayEncoderDecoder

            ColumnTypes.Inet -> InetAddressEncoderDecoder
            ColumnTypes.InetArray -> this.inetAddressArrayDecoder

            else -> StringEncoderDecoder
        }
    }

}
