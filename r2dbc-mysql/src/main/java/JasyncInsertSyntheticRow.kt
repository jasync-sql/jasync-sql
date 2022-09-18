package com.github.jasync.r2dbc.mysql

import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import java.math.BigDecimal
import java.math.BigInteger

/**
 * A synthetic row considers last inserted ID for support [JasyncStatement.returnGeneratedValues].
 *
 * @see JasyncResult
 * @see JasyncStatement
 * @see JasyncInsertSyntheticMetadata
 * @see io.r2dbc.spi.Statement
 */
internal class JasyncInsertSyntheticRow(private val generatedKeyName: String, private val lastInsertId: Long) : Row {

    override fun <T : Any?> get(index: Int, type: Class<T>): T? {
        return get(index as Any, type)
    }

    override fun <T : Any?> get(name: String, type: Class<T>): T? {
        return get(name as Any, type)
    }

    override fun getMetadata(): RowMetadata {
        TODO("Not yet implemented")
    }

    private fun <T : Any?> get(identifier: Any, type: Class<T>): T? {
        assertValidIdentifier(identifier)

        return getValue(type)
    }

    private fun get(identifier: Any): Any? {
        assertValidIdentifier(identifier)

        return if (lastInsertId < 0) {
            // BIGINT UNSIGNED and overflow than signed int64
            getValue(BigInteger::class.java)
        } else {
            getValue(Long::class.java)
        }
    }

    private fun <T> getValue(type: Class<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return when (type) {
            java.lang.Long::class.java, Long::class.java -> lastInsertId
            java.lang.Integer::class.java, Int::class.java -> lastInsertId.toInt()
            java.lang.Float::class.java, Float::class.java -> lastInsertId.toFloat()
            java.lang.Double::class.java, Double::class.java -> lastInsertId.toDouble()
            java.lang.Character::class.java, Char::class.java -> lastInsertId.toChar()
            java.lang.Short::class.java, Short::class.java -> lastInsertId.toShort()
            java.lang.Byte::class.java, Byte::class.java -> lastInsertId.toByte()
            java.math.BigDecimal::class.java -> if (lastInsertId < 0) {
                // BIGINT UNSIGNED and value is bigger than Long.MAX_VALUE
                BigDecimal(java.lang.Long.toUnsignedString(lastInsertId))
            } else {
                lastInsertId.toBigDecimal()
            }
            java.math.BigInteger::class.java -> if (lastInsertId < 0) {
                // BIGINT UNSIGNED and value is bigger than Long.MAX_VALUE
                BigInteger(java.lang.Long.toUnsignedString(lastInsertId))
            } else {
                lastInsertId.toBigInteger()
            }
            else -> {
                if (type.isAssignableFrom(Number::class.java)) {
                    if (lastInsertId < 0) {
                        BigInteger(java.lang.Long.toUnsignedString(lastInsertId))
                    } else {
                        lastInsertId
                    }
                } else {
                    throw IllegalStateException("unmatched requested type ${type.simpleName}")
                }
            }
        } as T
    }

    private fun assertValidIdentifier(identifier: Any) {
        when (identifier) {
            is Int -> {
                if (identifier != 0) {
                    throw IndexOutOfBoundsException("Index: $identifier, Size: 1")
                }
            }
            is String -> {
                if (!generatedKeyName.equals(identifier, true)) {
                    throw NoSuchElementException("Key $identifier is missing in the map.")
                }
            }
            else -> throw IllegalArgumentException("Identifier must be a String or an Integer")
        }
    }
}
