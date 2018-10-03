package com.github.jasync.sql.db

/**
 *
 * Represents a row from a database, allows clients to access rows by column number or column name.
 *
 */
interface RowData: List<Any?> {

  /**
   *
   * Returns a column value by it's position in the originating query.
   *
   * @param index
   * @return
   */

  override operator fun get(index: Int): Any?

  /**
   *
   * Returns a column value by it's name in the originating query.
   *
   * @param columnName
   * @return
   */

  operator fun get(columnName: String): Any?

  /**
   *
   * Number of this row in the query results. Counts start at 0.
   *
   * @return
   */

  fun rowNumber(): Int

}

/**
 *
 * Returns a column value by it's position in the originating query.
 *
 * @param columnNumber
 * @return
 */

operator fun RowData.invoke(columnNumber: Int): Any? = this.get(columnNumber)

/**
 *
 * Returns a column value by it's name in the originating query.
 *
 * @param columnName
 * @return
 */

operator fun RowData.invoke(columnName: String): Any? = this.get(columnName)

