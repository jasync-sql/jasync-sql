package com.github.jasync.sql.db.general

import com.github.jasync.sql.db.ResultSet
import com.github.jasync.sql.db.RowData

class MutableResultSet<T : ColumnData>(
    val columnTypes: List<T>,
    private val rows: MutableList<RowData> = mutableListOf()
) : ResultSet, List<RowData> by rows {

    private val columnMapping: Map<String, Int> =
        this.columnTypes.indices.map { index -> this.columnTypes[index].name to index }.toMap()

    override fun columnNames(): List<String> = this.columnTypes.map { c -> c.name }

    @Suppress("unused")
    val types: List<Int> = this.columnTypes.map { c -> c.dataType() }

    fun length(): Int = this.rows.size

    operator fun invoke(idx: Int): RowData = this.rows[idx]

    fun addRow(row: Array<Any?>) {
        this.rows += (ArrayRowData(this.rows.size, this.columnMapping, row))
    }
}
