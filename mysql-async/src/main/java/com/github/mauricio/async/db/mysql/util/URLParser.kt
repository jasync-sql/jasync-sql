package com.github.mauricio.async.db.mysql.util

import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.util.AbstractURIParser

/**
 * The MySQL URL parser.
 */
object URLParser : AbstractURIParser() {

  /**
   * The funault configuration for MySQL.
   */
  override val DEFAULT = Configuration(
    username = "root",
    host = "127.0.0.1", //Matched JDBC funault
    port = 3306,
    password = null,
    database = null
  )

  override val SCHEME = "^mysql$".toRegex()

}
