package com.github.jasync.r2dbc.mysql

import io.r2dbc.spi.ColumnMetadata
import io.r2dbc.spi.RowMetadata
import io.r2dbc.spi.Type
import kotlin.Any
import kotlin.IllegalArgumentException
import kotlin.Int
import kotlin.NoSuchElementException
import kotlin.String

/**
 * An implementation of [RowMetadata] for support [JasyncStatement.returnGeneratedValues].
 *
 * It is also an implementation of [ColumnMetadata] for reduce redundant objects.
 *
 * @see JasyncInsertSyntheticRow
 */
internal class JasyncInsertSyntheticMetadata(private val generatedKeyName: String) : RowMetadata, ColumnMetadata {

    override fun getColumnMetadata(index: Int): ColumnMetadata {
        assertValidIdentifier(index)
        return this
    }

    override fun getColumnMetadata(name: String): ColumnMetadata {
        assertValidIdentifier(name)
        return this
    }

    override fun getColumnMetadatas(): MutableList<out ColumnMetadata> {
        return mutableListOf(this)
    }

    override fun getType(): Type {
        TODO("Not yet implemented")
    }

    override fun getName(): String {
        return generatedKeyName
    }

    private fun assertValidIdentifier(identifier: Any) {
        when (identifier) {
            is Int -> {
                when {
                    identifier > 0 -> throw ArrayIndexOutOfBoundsException("Column index $identifier is larger than the number of columns 1")
                    identifier < 0 -> throw ArrayIndexOutOfBoundsException("Column index $identifier is negative")
                }
            }
            is String -> {
                if (!generatedKeyName.equals(identifier, true)) {
                    throw NoSuchElementException("Column name '$identifier' does not exist in column names [$generatedKeyName]")
                }
            }
            else -> throw IllegalArgumentException("Identifier '$identifier' is not a valid identifier. Should either be an Integer index or a String column name.")
        }
    }
}
