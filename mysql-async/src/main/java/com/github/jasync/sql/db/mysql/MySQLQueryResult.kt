package com.github.jasync.sql.db.mysql

import com.github.jasync.sql.db.EMPTY_RESULT_SET
import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.ResultSet

class MySQLQueryResult(
    rowsAffected: Long,
    message: String?,
    val lastInsertId: Long,
    val statusFlags: Int,
    val warnings: Int,
    rows: ResultSet = EMPTY_RESULT_SET) : QueryResult(rowsAffected, message, rows) {

  override fun toString(): String {
    return "MySQLQueryResult{rows -> $rowsAffected,lastInsertId -> $lastInsertId}"
  }

  fun toStringDebug(): String {
    return "MySQLQueryResult{rows -> $rowsAffected,status -> $statusMessage($statusFlags,$warnings),lastInsertId -> $lastInsertId}"
  }
}
