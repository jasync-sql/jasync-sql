package com.github.jasync.sql.db.postgresql.column

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
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.buffer.ByteBuf
import io.netty.util.CharsetUtil
import java.nio.charset.Charset

private val logger = KotlinLogging.logger {}

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

    private val registry: MutableMap<Int, ColumnDecoder> = defaultRegistry()

    /**
     * Add custom decoder
     */
    fun registerDecoder(type: Int, decoder: ColumnDecoder) {
        registry[type] = decoder
        logger.info { "register decoder $type $decoder" }
    }

    override fun decode(kind: ColumnData, value: ByteBuf, charset: Charset): Any {
        return registry.getOrDefault(kind.dataType(), StringEncoderDecoder).decode(kind, value, charset)!!
    }

    private fun defaultRegistry(): MutableMap<Int, ColumnDecoder> {
        val res = mutableMapOf<Int, ColumnDecoder>()
        res[ColumnTypes.Boolean] = BooleanEncoderDecoder
        res[ColumnTypes.BooleanArray] = this.booleanArrayDecoder

        res[ColumnTypes.Char] = CharEncoderDecoder
        res[ColumnTypes.CharArray] = this.charArrayDecoder

        res[ColumnTypes.Bigserial] = LongEncoderDecoder
        res[ColumnTypes.BigserialArray] = this.longArrayDecoder

        res[ColumnTypes.Smallint] = ShortEncoderDecoder
        res[ColumnTypes.SmallintArray] = this.shortArrayDecoder

        res[ColumnTypes.Integer] = IntegerEncoderDecoder
        res[ColumnTypes.IntegerArray] = this.integerArrayDecoder

        res[ColumnTypes.OID] = LongEncoderDecoder
        res[ColumnTypes.OIDArray] = this.longArrayDecoder

        res[ColumnTypes.Numeric] = BigDecimalEncoderDecoder
        res[ColumnTypes.NumericArray] = this.bigDecimalArrayDecoder

        res[ColumnTypes.Real] = FloatEncoderDecoder
        res[ColumnTypes.RealArray] = this.floatArrayDecoder

        res[ColumnTypes.Double] = DoubleEncoderDecoder
        res[ColumnTypes.DoubleArray] = this.doubleArrayDecoder

        res[ColumnTypes.Text] = StringEncoderDecoder
        res[ColumnTypes.TextArray] = this.stringArrayDecoder

        res[ColumnTypes.Varchar] = StringEncoderDecoder
        res[ColumnTypes.VarcharArray] = this.stringArrayDecoder

        res[ColumnTypes.Bpchar] = StringEncoderDecoder
        res[ColumnTypes.BpcharArray] = this.stringArrayDecoder

        res[ColumnTypes.Timestamp] = PostgreSQLTimestampEncoderDecoder
        res[ColumnTypes.TimestampArray] = this.timestampArrayDecoder

        res[ColumnTypes.TimestampWithTimezone] = PostgreSQLTimestampEncoderDecoder
        res[ColumnTypes.TimestampWithTimezoneArray] = this.timestampWithTimezoneArrayDecoder

        res[ColumnTypes.Date] = DateEncoderDecoder
        res[ColumnTypes.DateArray] = this.dateArrayDecoder

        res[ColumnTypes.Time] = TimeEncoderDecoder.Instance
        res[ColumnTypes.TimeArray] = this.timeArrayDecoder

        res[ColumnTypes.TimeWithTimezone] = TimeWithTimezoneEncoderDecoder
        res[ColumnTypes.TimeWithTimezoneArray] = this.timeWithTimestampArrayDecoder

        res[ColumnTypes.Interval] = PostgreSQLIntervalEncoderDecoder
        res[ColumnTypes.IntervalArray] = this.intervalArrayDecoder

        res[ColumnTypes.MoneyArray] = this.stringArrayDecoder
        res[ColumnTypes.NameArray] = this.stringArrayDecoder
        res[ColumnTypes.UUID] = UUIDEncoderDecoder
        res[ColumnTypes.UUIDArray] = this.uuidArrayDecoder
        res[ColumnTypes.XMLArray] = this.stringArrayDecoder
        res[ColumnTypes.ByteA] = ByteArrayEncoderDecoder

        res[ColumnTypes.Inet] = InetAddressEncoderDecoder
        res[ColumnTypes.InetArray] = this.inetAddressArrayDecoder

        return res
    }
}
