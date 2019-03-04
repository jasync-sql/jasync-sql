package com.github.jasync.r2dbc.mysql

import com.github.jasync.sql.db.ResultSet
import io.r2dbc.spi.ColumnMetadata
import io.r2dbc.spi.RowMetadata
import java.util.*


class JasyncMetadata(rows: ResultSet) : RowMetadata {

    private val columnNames: List<String> = rows.columnNames()
    private val metadata: Map<String, ColumnMetadata> = columnNames.map { it to JasyncColumnMetadata(it) }.toMap()

    override fun getColumnMetadata(identifier: Any): ColumnMetadata {

        if (identifier is String) {
            val metadata = this.metadata[identifier] ?: throw NoSuchElementException(
                String
                    .format("Column name '%s' does not exist in column names %s", identifier, columnNames)
            )
        }

        if (identifier is Int) {
            if (identifier >= this.columnNames.size) {
                throw ArrayIndexOutOfBoundsException(
                    String
                        .format(
                            "Column index %d is larger than the number of columns %d", identifier, columnNames
                                .size
                        )
                )
            }

            if (0 > identifier) {
                throw ArrayIndexOutOfBoundsException(
                    String
                        .format("Column index %d is negative", identifier)
                )
            }

            return this.metadata.getValue(columnNames[0])
        }

        throw IllegalArgumentException(
            String
                .format(
                    "Identifier '%s' is not a valid identifier. Should either be an Integer index or a String column name.",
                    identifier
                )
        )
    }

    override fun getColumnMetadatas(): Iterable<ColumnMetadata> {
        return metadata.values
    }


    internal class JasyncColumnMetadata(private val name: String) : ColumnMetadata {

        override fun getName(): String {
            return name
        }
    }
}