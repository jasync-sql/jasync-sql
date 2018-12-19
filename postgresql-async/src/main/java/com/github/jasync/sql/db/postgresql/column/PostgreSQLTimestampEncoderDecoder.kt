package com.github.jasync.sql.db.postgresql.column

import com.github.jasync.sql.db.column.ColumnEncoderDecoder
import com.github.jasync.sql.db.exceptions.DateEncoderNotAvailableException
import com.github.jasync.sql.db.general.ColumnData
import com.github.jasync.sql.db.postgresql.messages.backend.PostgreSQLColumnData
import com.github.jasync.sql.db.util.XXX
import io.netty.buffer.ByteBuf
import mu.KotlinLogging
import org.joda.time.DateTime
import org.joda.time.LocalDateTime
import org.joda.time.ReadableDateTime
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.DateTimeFormatterBuilder
import java.nio.charset.Charset
import java.sql.Timestamp
import java.util.*

private val logger = KotlinLogging.logger {}

object PostgreSQLTimestampEncoderDecoder : ColumnEncoderDecoder {


    private val optionalTimeZone = DateTimeFormatterBuilder()
        .appendPattern("Z").toParser()

    private val internalFormatters: List<DateTimeFormatter> = (1..6).map { index ->
        DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .appendPattern("." + ("S".repeat(index)))
            .appendOptional(optionalTimeZone)
            .toFormatter()
    }

    private val internalFormatterWithoutSeconds = DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd HH:mm:ss")
        .appendOptional(optionalTimeZone)
        .toFormatter()

    fun formatter() = internalFormatters[5]

    override fun decode(kind: ColumnData, value: ByteBuf, charset: Charset): Any {
        val bytes = ByteArray(value.readableBytes())
        value.readBytes(bytes)

        val text = String(bytes, charset)

        val columnType = kind as PostgreSQLColumnData

        return when (columnType.dataType) {
            ColumnTypes.Timestamp, ColumnTypes.TimestampArray -> {
                selectFormatter(text).parseLocalDateTime(text)
            }
            ColumnTypes.TimestampWithTimezoneArray -> {
                selectFormatter(text).parseDateTime(text)
            }
            ColumnTypes.TimestampWithTimezone -> {
                if (columnType.dataTypeModifier > 0) {
                    internalFormatters[columnType.dataTypeModifier - 1].parseDateTime(text)
                } else {
                    selectFormatter(text).parseDateTime(text)
                }
            }
            else -> XXX("should treat ${columnType.dataType}")
        }
    }

    private fun selectFormatter(text: String): DateTimeFormatter {
        return if (text.contains(".")) {
            internalFormatters[5]
        } else {
            internalFormatterWithoutSeconds
        }
    }

    override fun decode(value: String): Any =
        throw UnsupportedOperationException("this method should not have been called")

    override fun encode(value: Any): String {
        return when (value) {
            is Timestamp -> this.formatter().print(DateTime(value))
            is Date -> this.formatter().print(DateTime(value))
            is Calendar -> this.formatter().print(DateTime(value))
            is LocalDateTime -> this.formatter().print(value)
            is ReadableDateTime -> this.formatter().print(value)
            else -> throw DateEncoderNotAvailableException(value)
        }
    }

    override fun supportsStringDecoding(): Boolean = false

}
