package com.github.jasync.sql.db.general

import com.github.jasync.sql.db.RowData

class ArrayRowData(val row: Int, val mapping: Map<String, Int>, val columns: Array<Any?>) : RowData {

  /**
   *
   * Returns a column value by it's position in the originating query.
   *
   * @param columnNumber
   * @return
   */
  override fun get(columnNumber: Int): Any? = columns[columnNumber]

  /**
   *
   * Returns a column value by it's name in the originating query.
   *
   * @param columnName
   * @return
   */
  override fun get(columnName: String): Any? = columns[mapping.getValue(columnName)]

  /**
   *
   * Number of this row in the query results. Counts start at 0.
   *
   * @return
   */
  override fun rowNumber(): Int = row

  fun length(): Int = columns.size
}
