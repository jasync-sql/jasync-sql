package com.github.jasync.sql.db.postgresql.column

import com.github.jasync.sql.db.column.ColumnEncoderDecoder
import com.github.jasync.sql.db.exceptions.DateEncoderNotAvailableException
import com.github.jasync.sql.db.general.ColumnData
import com.github.jasync.sql.db.postgresql.messages.backend.PostgreSQLColumnData
import com.github.jasync.sql.db.util.XXX
import com.github.jasync.sql.db.util.microsecondsFormatter
import io.netty.buffer.ByteBuf
import java.nio.charset.Charset
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.TemporalAccessor
import java.util.Calendar
import java.util.Date

object PostgreSQLTimestampEncoderDecoder : ColumnEncoderDecoder {

    private val optionalTimeZone = DateTimeFormatterBuilder()
        .appendPattern("X").toFormatter()

    private val internalFormatter = DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd HH:mm:ss")
        .appendOptional(microsecondsFormatter)
        .appendOptional(optionalTimeZone)
        .toFormatter()

    private val internalFormatterWithoutSeconds = DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd HH:mm:ss")
        .appendOptional(optionalTimeZone)
        .toFormatter()

    fun formatter() = internalFormatter

    override fun decode(kind: ColumnData, value: ByteBuf, charset: Charset): Any {
        val bytes = ByteArray(value.readableBytes())
        value.readBytes(bytes)

        val text = String(bytes, charset)

        val columnType = kind as PostgreSQLColumnData

        return when (columnType.dataType) {
            ColumnTypes.Timestamp, ColumnTypes.TimestampArray -> {
                LocalDateTime.parse(text, selectFormatter(text))
            }
            ColumnTypes.TimestampWithTimezoneArray -> {
                OffsetDateTime.parse(text, selectFormatter(text))
            }
            ColumnTypes.TimestampWithTimezone -> {
                if (columnType.dataTypeModifier > 0) {
                    OffsetDateTime.parse(text, internalFormatter)
                } else {
                    OffsetDateTime.parse(text, selectFormatter(text))
                }
            }
            else -> XXX("should treat ${columnType.dataType}")
        }
    }

    private fun selectFormatter(text: String): DateTimeFormatter {
        return if (text.contains(".")) {
            internalFormatter
        } else {
            internalFormatterWithoutSeconds
        }
    }

    override fun decode(value: String): Any =
        throw UnsupportedOperationException("this method should not have been called")

    override fun encode(value: Any): String {
        return when (value) {
            is Timestamp -> value.toInstant().atOffset(ZoneOffset.UTC).format(this.formatter())
            is Date -> value.toInstant().atOffset(ZoneOffset.UTC).format(this.formatter())
            is Calendar -> value.toInstant().atOffset(ZoneOffset.UTC).format(this.formatter())
            is LocalDateTime -> this.formatter().format(value)
            is TemporalAccessor -> this.formatter().format(value)
            else -> throw DateEncoderNotAvailableException(value)
        }
    }

    override fun supportsStringDecoding(): Boolean = false
}
