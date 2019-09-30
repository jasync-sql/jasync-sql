package com.github.jasync.sql.db.column

import com.github.jasync.sql.db.exceptions.DateEncoderNotAvailableException
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.Temporal
import java.time.temporal.TemporalAccessor
import java.util.*


open class TimestampEncoderDecoder : ColumnEncoderDecoder {
    companion object {
        const val BaseFormat = "yyyy-MM-dd HH:mm:ss"
        const val MillisFormat = ".SSSSSS"
        val Instance = TimestampEncoderDecoder()

    }

    private val optional = DateTimeFormatterBuilder()
        .appendPattern(MillisFormat).toFormatter()
    private val optionalTimeZone = DateTimeFormatterBuilder()
        .appendPattern("Z").toFormatter()

    private val builder = DateTimeFormatterBuilder()
        .appendPattern(BaseFormat)
        .appendOptional(optional)
        .appendOptional(optionalTimeZone)

    private val timezonedPrinter = DateTimeFormatterBuilder()
        .appendPattern("${BaseFormat}${MillisFormat}Z").toFormatter()

    private val nonTimezonedPrinter = DateTimeFormatterBuilder()
        .appendPattern("${BaseFormat}${MillisFormat}").toFormatter()

    private val format = builder.toFormatter()

    open fun formatter() = format

    override fun decode(value: String): Any {
        return LocalDateTime.parse(value, formatter())
    }

    override fun encode(value: Any): String {
        return when (value) {
            is Timestamp -> value.toLocalDateTime().format(this.timezonedPrinter)
            is Date -> LocalDateTime.ofInstant(value.toInstant(), ZoneOffset.UTC).format(this.timezonedPrinter)
            is Calendar -> LocalDateTime.ofInstant(value.toInstant(), ZoneOffset.UTC).format(this.timezonedPrinter)
            is LocalDateTime -> value.format(this.nonTimezonedPrinter)
            is TemporalAccessor -> this.timezonedPrinter.format(value)
            else -> throw DateEncoderNotAvailableException(value)
        }
    }

}
