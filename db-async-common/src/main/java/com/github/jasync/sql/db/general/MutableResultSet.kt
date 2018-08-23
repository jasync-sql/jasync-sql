package com.github.jasync.sql.db.general

import com.github.jasync.sql.db.RowData
import com.github.jasync.sql.db.ResultSet
import com.github.jasync.sql.db.util.Log


class MutableResultSet<T : ColumnData>(
    val columnTypes: List<T>) : ResultSet {

  companion object {
    val log = Log.get()

  }

  private val rows = mutableListOf<RowData>()
  private val columnMapping: Map<String, Int> = this.columnTypes.indices.map { index -> this.columnTypes[index].name() to index }.toMap()


  override fun columnNames(): List<String> = this.columnTypes.map { c -> c.name() }

  val types: List<Int> = this.columnTypes.map { c -> c.dataType() }

  fun length(): Int = this.rows.size //override

  fun invoke(idx: Int): RowData = this.rows[idx] //override

  fun addRow(row: Array<Any>) {
    this.rows += (ArrayRowData(this.rows.size, this.columnMapping, row))
  }

}
