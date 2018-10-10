package com.github.jasync.sql.db

import com.github.jasync.sql.db.util.XXX
import org.joda.time.LocalDateTime

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
   * @param column
   * @return
   */

  operator fun get(column: String): Any?

  /**
   *
   * Number of this row in the query results. Counts start at 0.
   *
   * @return
   */

  fun rowNumber(): Int

  fun getInt(column: Int): Int? = get(column) as Int?

  fun getInt(column: String): Int? = get(column) as Int?

  fun getLong(column: Int): Long? = get(column) as Long?

  fun getLong(column: String): Long? = get(column) as Long?

  fun getBoolean(column: Int): Boolean? = getBooleanFromRaw(get(column))

  fun getBoolean(column: String): Boolean? = getBooleanFromRaw(get(column))

  private fun getBooleanFromRaw(rawValue: Any?): Boolean? {
    return when (rawValue) {
      is Boolean? -> rawValue
      is ByteArray -> rawValue[0] == 1.toByte()
      is Byte -> rawValue == 1.toByte()
      else -> XXX("unsupported boolean type ${rawValue?.javaClass} for value $rawValue")
    }
  }

  fun getByte(column: String): Byte? = get(column) as Byte?

  fun getByte(column: Int): Byte? = get(column) as Byte?

  fun getDate(column: Int): LocalDateTime? = get(column) as LocalDateTime?

  fun getDate(column: String): LocalDateTime? = get(column) as LocalDateTime?

  fun getFloat(column: Int): Float? = get(column) as Float?

  fun getFloat(column: String): Float? = get(column) as Float?

  fun getDouble(column: Int): Double? = get(column) as Double?

  fun getDouble(column: String): Double? = get(column) as Double?

  fun getString(column: Int): String? = get(column) as String?

  fun getString(column: String): String? = get(column) as String?

}

@Suppress("UNCHECKED_CAST")
fun <T> RowData.getAs(column: Int): T = this[column] as T

@Suppress("UNCHECKED_CAST")
fun <T> RowData.getAs(column: String): T = this[column] as T

/**
 *
 * Returns a column value by it's position in the originating query.
 *
 * @param column - the number of the column by it's position in the originating query
 * @return
 */

operator fun RowData.invoke(column: Int): Any? = this[column]

/**
 *
 * Returns a column value by it's name in the originating query.
 *
 * @param column - the name of the column
 * @return
 */

operator fun RowData.invoke(column: String): Any? = this[column]

