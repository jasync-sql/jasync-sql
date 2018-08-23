package com.github.jasync.sql.db

/**
 *
 * Represents a row from a database, allows clients to access rows by column number or column name.
 *
 */
interface RowData {

  /**
   *
   * Returns a column value by it's position in the originating query.
   *
   * @param columnNumber
   * @return
   */

  operator fun invoke(columnNumber: Int): Any

  /**
   *
   * Returns a column value by it's name in the originating query.
   *
   * @param columnName
   * @return
   */

  operator fun invoke(columnName: String): Any

  /**
   *
   * Number of this row in the query results. Counts start at 0.
   *
   * @return
   */

  fun rowNumber(): Int

}
