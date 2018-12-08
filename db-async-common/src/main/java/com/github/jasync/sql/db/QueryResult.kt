package com.github.jasync.sql.db

/**
 *
 * This is the result of the execution of a statement, contains basic information as the number or rows
 * affected by the statement and the rows returned if there were any.
 *
 * @param rowsAffected
 * @param statusMessage
 * @param rows
 */

open class QueryResult(val rowsAffected: Long, val statusMessage: String?, val rows: ResultSet = EMPTY_RESULT_SET) {

  override fun toString() : String {
    return "QueryResult{rows -> %s,status -> %s}".format(this.rowsAffected, this.statusMessage)
  }

}
