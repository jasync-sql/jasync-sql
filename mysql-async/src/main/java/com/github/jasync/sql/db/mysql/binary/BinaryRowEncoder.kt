package com.github.jasync.sql.db.mysql.binary

import com.github.jasync.sql.db.mysql.binary.encoder.BinaryEncoder
import com.github.jasync.sql.db.mysql.binary.encoder.BooleanEncoder
import com.github.jasync.sql.db.mysql.binary.encoder.ByteArrayEncoder
import com.github.jasync.sql.db.mysql.binary.encoder.ByteBufEncoder
import com.github.jasync.sql.db.mysql.binary.encoder.ByteBufferEncoder
import com.github.jasync.sql.db.mysql.binary.encoder.ByteEncoder
import com.github.jasync.sql.db.mysql.binary.encoder.CalendarEncoder
import com.github.jasync.sql.db.mysql.binary.encoder.DateTimeEncoder
import com.github.jasync.sql.db.mysql.binary.encoder.DoubleEncoder
import com.github.jasync.sql.db.mysql.binary.encoder.DurationEncoder
import com.github.jasync.sql.db.mysql.binary.encoder.FloatEncoder
import com.github.jasync.sql.db.mysql.binary.encoder.IntegerEncoder
import com.github.jasync.sql.db.mysql.binary.encoder.JavaDateEncoder
import com.github.jasync.sql.db.mysql.binary.encoder.LocalDateEncoder
import com.github.jasync.sql.db.mysql.binary.encoder.LocalDateTimeEncoder
import com.github.jasync.sql.db.mysql.binary.encoder.LocalTimeEncoder
import com.github.jasync.sql.db.mysql.binary.encoder.LongEncoder
import com.github.jasync.sql.db.mysql.binary.encoder.ReadableInstantEncoder
import com.github.jasync.sql.db.mysql.binary.encoder.SQLDateEncoder
import com.github.jasync.sql.db.mysql.binary.encoder.SQLTimeEncoder
import com.github.jasync.sql.db.mysql.binary.encoder.SQLTimestampEncoder
import com.github.jasync.sql.db.mysql.binary.encoder.ShortEncoder
import com.github.jasync.sql.db.mysql.binary.encoder.StringEncoder
import com.github.jasync.sql.db.util.XXX
import io.netty.buffer.ByteBuf
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime
import org.joda.time.LocalTime
import org.joda.time.ReadableDateTime
import org.joda.time.ReadableInstant
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.time.Duration


class BinaryRowEncoder(charset: Charset) {

    private val stringEncoder = StringEncoder(charset)
    private val encoders: Map<Class<*>, BinaryEncoder> = mapOf(
        String::class.java to this.stringEncoder,
        BigInteger::class.java to this.stringEncoder,
        BigDecimal::class.java to this.stringEncoder,
        java.math.BigDecimal::class.java to this.stringEncoder,
        java.math.BigInteger::class.java to this.stringEncoder,
        Byte::class.java to ByteEncoder,
        java.lang.Byte::class.java to ByteEncoder,
        Short::class.java to ShortEncoder,
        java.lang.Short::class.java to ShortEncoder,
        Int::class.java to IntegerEncoder,
        java.lang.Integer::class.java to IntegerEncoder,
        Long::class.java to LongEncoder,
        java.lang.Long::class.java to LongEncoder,
        Float::class.java to FloatEncoder,
        java.lang.Float::class.java to FloatEncoder,
        Double::class.java to DoubleEncoder,
        java.lang.Double::class.java to DoubleEncoder,
        LocalDateTime::class.java to LocalDateTimeEncoder,
        DateTime::class.java to DateTimeEncoder,
        LocalDate::class.java to LocalDateEncoder,
        java.util.Date::class.java to JavaDateEncoder,
        java.sql.Timestamp::class.java to SQLTimestampEncoder,
        java.sql.Date::class.java to SQLDateEncoder,
        java.sql.Time::class.java to SQLTimeEncoder,
        Duration::class.java to DurationEncoder,
        ByteArray::class.java to ByteArrayEncoder,
        Boolean::class.java to BooleanEncoder,
        java.lang.Boolean::class.java to BooleanEncoder
    )

    fun encoderFor(v: Any): BinaryEncoder {

        return this.encoders.getOrElse(v::class.java) {
            return when (v) {
                is CharSequence -> this.stringEncoder
                is java.math.BigInteger -> this.stringEncoder
                is BigDecimal -> this.stringEncoder
                is java.math.BigDecimal -> this.stringEncoder
                is ReadableDateTime -> DateTimeEncoder
                is ReadableInstant -> ReadableInstantEncoder
                is LocalDateTime -> LocalDateTimeEncoder
                is java.sql.Timestamp -> SQLTimestampEncoder
                is java.sql.Date -> SQLDateEncoder
                is java.util.Calendar -> CalendarEncoder
                is LocalDate -> LocalDateEncoder
                is LocalTime -> LocalTimeEncoder
                is java.sql.Time -> SQLTimeEncoder
                is Duration -> DurationEncoder
                is java.util.Date -> JavaDateEncoder
                is ByteBuffer -> ByteBufferEncoder
                is ByteBuf -> ByteBufEncoder
                else -> XXX("couldn't find mapping for ${v::class.java}")
            }
        }
    }

}


