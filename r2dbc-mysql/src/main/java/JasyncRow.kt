package com.github.jasync.r2dbc.mysql

import com.github.jasync.sql.db.RowData
import io.r2dbc.spi.Row
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime


class JasyncRow(private val rowData: RowData) : Row {


    @Suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
    override fun <T> get(identifier: Any, requestedType: Class<T>): T? {
        val value = get(identifier)
        return when {
            requestedType == String::class.java -> value?.toString()
            value is Number -> {
                when (requestedType) {
                    Long::class.java -> value.toLong()
                    Int::class.java -> value.toInt()
                    Float::class.java -> value.toFloat()
                    Double::class.java -> value.toDouble()
                    Char::class.java -> value.toChar()
                    Short::class.java -> value.toShort()
                    Byte::class.java -> value.toByte()
                    else -> throw IllegalStateException("unmatched requested type ${requestedType.simpleName}")
                }
            }
            value is org.joda.time.LocalDateTime -> {
                when (requestedType) {
                    LocalDate::class.java -> value.toLocalDate().jodaToJavaLocalDate()
                    LocalDateTime::class.java -> value.jodaToJavaLocalDateTime()
                    LocalTime::class.java -> value.toLocalTime().jodaToJavaLocalTime()
                    else -> throw IllegalStateException("unmatched requested type ${requestedType.simpleName}")
                }
            }
            value is org.joda.time.LocalDate -> {
                when (requestedType) {
                    LocalDate::class.java -> value.jodaToJavaLocalDate()
                    LocalDateTime::class.java -> value.toLocalDateTime(org.joda.time.LocalTime.MIDNIGHT).jodaToJavaLocalDateTime()
                    LocalTime::class.java -> org.joda.time.LocalTime.MIDNIGHT.jodaToJavaLocalTime()
                    else -> throw IllegalStateException("unmatched requested type ${requestedType.simpleName}")
                }
            }
            value is org.joda.time.LocalTime -> {
                when (requestedType) {
                    LocalTime::class.java -> value.jodaToJavaLocalTime()
                    else -> throw IllegalStateException("unmatched requested type ${requestedType.simpleName}")
                }
            }
            else -> requestedType.cast(value)
        }  as T?
    }

    override fun get(identifier: Any): Any? {
        return when (identifier) {
            is String -> rowData[identifier]
            is Int -> rowData[identifier]
            else -> throw IllegalArgumentException("Identifier must be a String or an Integer")
        }
    }
}

