package com.github.jasync.r2dbc.mysql

import io.r2dbc.spi.ColumnMetadata
import io.r2dbc.spi.RowMetadata

/**
 * An implementation of [RowMetadata] for support [JasyncStatement.returnGeneratedValues].
 *
 * It is also an implementation of [ColumnMetadata] for reduce redundant objects.
 *
 * @see JasyncInsertFakeRow
 */
internal class JasyncInsertFakeMetadata(private val generatedKeyName: String) : RowMetadata, ColumnMetadata {

    override fun getColumnMetadatas(): Iterable<ColumnMetadata> {
        return listOf(this)
    }

    override fun getColumnMetadata(identifier: Any): ColumnMetadata {
        return when (identifier) {
            is Int -> {
                when {
                    identifier > 0 -> throw ArrayIndexOutOfBoundsException("Column index $identifier is larger than the number of columns 1")
                    identifier < 0 -> throw ArrayIndexOutOfBoundsException("Column index $identifier is negative")
                    else -> this
                }
            }
            is String -> {
                if (identifier != generatedKeyName) {
                    throw NoSuchElementException("Column name '$identifier' does not exist in column names {$generatedKeyName}")
                }

                this
            }
            else -> throw IllegalArgumentException("Identifier '$identifier' is not a valid identifier. Should either be an Integer index or a String column name.")
        }
    }

    override fun getColumnNames(): Collection<String> {
        return setOf(generatedKeyName)
    }

    override fun getName(): String {
        return generatedKeyName
    }
}
