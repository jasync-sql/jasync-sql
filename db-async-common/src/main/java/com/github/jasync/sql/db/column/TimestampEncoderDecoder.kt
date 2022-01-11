package com.github.jasync.sql.db.column

import com.github.jasync.sql.db.exceptions.DateEncoderNotAvailableException
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.TemporalAccessor
import java.util.Calendar
import java.util.Date

open class TimestampEncoderDecoder : ColumnEncoderDecoder {
    companion object {
        const val BaseFormat = "yyyy-MM-dd HH:mm:ss"
        const val MillisFormat = ".SSSSSS"
        val Instance = TimestampEncoderDecoder()
    }

    private val optional = DateTimeFormatterBuilder()
        .appendPattern(MillisFormat).toFormatter()
    private val optionalTimeZone = DateTimeFormatterBuilder()
        .appendPattern("[X][Z]").toFormatter()

    private val builder = DateTimeFormatterBuilder()
        .appendPattern(BaseFormat)
        .appendOptional(optional)
        .appendOptional(optionalTimeZone)

    private val timezonedPrinter = DateTimeFormatterBuilder()
        .appendPattern("${BaseFormat}${MillisFormat}Z").toFormatter()

    private val nonTimezonedPrinter = DateTimeFormatterBuilder()
        .appendPattern("${BaseFormat}$MillisFormat").toFormatter()

    private val format = builder.toFormatter()

    // java.util.Dates are constructed using the system default timezone, replicate this behavior when encoding a legacy date
    private fun encodeLegacyDate(legacyDate: Date): String =
        legacyDate.toInstant().atOffset(ZoneOffset.UTC).format(this.timezonedPrinter)

    open fun formatter() = format

    override fun decode(value: String): Any {
        return LocalDateTime.parse(value, formatter())
    }

    override fun encode(value: Any): String {
        return when (value) {
            is Timestamp -> encodeLegacyDate(value)
            is Date -> encodeLegacyDate(value)
            is Calendar -> encodeLegacyDate(value.time)
            is LocalDateTime -> this.nonTimezonedPrinter.format(value)
            is TemporalAccessor -> this.timezonedPrinter.format(value)
            else -> throw DateEncoderNotAvailableException(value)
        }
    }
}
