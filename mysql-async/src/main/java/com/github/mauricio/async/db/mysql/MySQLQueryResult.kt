
package com.github.mauricio.async.db.mysql

import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.ResultSet

class MySQLQueryResult(
  rowsAffected: Long,
  message: String?,
  val lastInsertId: Long,
  val statusFlags: Int,
  val warnings: Int,
  rows: ResultSet? = null) : QueryResult(rowsAffected, message, rows)
