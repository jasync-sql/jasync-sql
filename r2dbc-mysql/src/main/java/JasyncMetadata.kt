package com.github.jasync.r2dbc.mysql

import com.github.jasync.sql.db.ResultSet
import io.r2dbc.spi.ColumnMetadata
import io.r2dbc.spi.RowMetadata
import io.r2dbc.spi.Type

class JasyncMetadata(rows: ResultSet) : RowMetadata {
    override fun getColumnMetadata(index: Int): ColumnMetadata {
        if (index >= this.columnNames.size) {
            throw ArrayIndexOutOfBoundsException(
                String
                    .format(
                        "Column index %d is larger than the number of columns %d", index, columnNames
                            .size
                    )
            )
        }

        if (0 > index) {
            throw ArrayIndexOutOfBoundsException(
                String
                    .format("Column index %d is negative", index)
            )
        }

        return this.metadata.getValue(columnNames[0])
    }

    override fun getColumnMetadata(name: String): ColumnMetadata {
        return this.metadata[name]
            ?: throw NoSuchElementException(
                String
                    .format("Column name '%s' does not exist in column names %s", name, columnNames)
            )
    }

    private val columnNames: List<String> = rows.columnNames()
    private val metadata: Map<String, ColumnMetadata> = columnNames.map { it to JasyncColumnMetadata(it) }.toMap()

    override fun getColumnMetadatas(): MutableList<out ColumnMetadata> {
        return metadata.values.toMutableList()
    }

    internal class JasyncColumnMetadata(private val name: String) : ColumnMetadata {
        override fun getType(): Type {
            TODO("Not yet implemented")
        }

        override fun getName(): String {
            return name
        }
    }

    override fun getColumnNames(): Collection<String> = columnNames
}
