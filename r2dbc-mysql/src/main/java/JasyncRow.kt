package com.github.jasync.r2dbc.mysql

import com.github.jasync.sql.db.RowData
import io.r2dbc.spi.Result
import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class JasyncRow(private val rowData: RowData, private val metadata: JasyncMetadata) : Row, Result.RowSegment {

    override fun <T : Any?> get(index: Int, type: Class<T>): T? {
        return get(index as Any, type)
    }

    override fun <T : Any?> get(name: String, type: Class<T>): T? {
        return get(name as Any, type)
    }

    override fun getMetadata(): RowMetadata {
        return metadata
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> get(identifier: Any, requestedType: Class<T>): T? {
        val value = get(identifier)
        return when {
            requestedType == Object::class.java -> value
            requestedType == String::class.java -> value?.toString()
            value is Number -> {
                when (requestedType) {
                    java.lang.Long::class.java -> value.toLong()
                    java.lang.Integer::class.java -> value.toInt()
                    java.lang.Float::class.java -> value.toFloat()
                    java.lang.Double::class.java -> value.toDouble()
                    java.lang.Character::class.java -> value.toChar()
                    java.lang.Short::class.java -> value.toShort()
                    java.lang.Byte::class.java -> value.toByte()
                    java.math.BigDecimal::class.java ->
                        if (value is BigDecimal) {
                            value
                        } else {
                            BigDecimal.valueOf(value.toDouble())
                        }
                    java.math.BigInteger::class.java ->
                        if (value is BigInteger) {
                            value
                        } else {
                            BigInteger.valueOf(value.toLong())
                        }
                    else -> throw IllegalStateException("unmatched requested type ${requestedType.simpleName}")
                }
            }
            value is LocalDateTime -> {
                when (requestedType) {
                    LocalDate::class.java -> value.toLocalDate()
                    LocalDateTime::class.java -> value
                    LocalTime::class.java -> value.toLocalTime()
                    else -> throw IllegalStateException("unmatched requested type ${requestedType.simpleName}")
                }
            }
            value is LocalDate -> {
                when (requestedType) {
                    LocalDate::class.java -> value
                    LocalDateTime::class.java -> value.atStartOfDay()
                    LocalTime::class.java -> java.time.LocalTime.MIDNIGHT
                    else -> throw IllegalStateException("unmatched requested type ${requestedType.simpleName}")
                }
            }
            value is LocalTime -> {
                when (requestedType) {
                    LocalTime::class.java -> value
                    else -> throw IllegalStateException("unmatched requested type ${requestedType.simpleName}")
                }
            }
            else -> requestedType.cast(value)
        } as T?
    }

    private fun get(identifier: Any): Any? {
        val value = when (identifier) {
            is String -> rowData[identifier]
            is Int -> rowData[identifier]
            else -> throw IllegalArgumentException("Identifier must be a String or an Integer")
        }
        return when (value) {
            is org.joda.time.LocalDateTime -> value.jodaToJavaLocalDateTime()
            is org.joda.time.LocalDate -> value.jodaToJavaLocalDate()
            is org.joda.time.LocalTime -> value.jodaToJavaLocalTime()
            else -> value
        }
    }

    override fun row(): Row {
        return this
    }
}
