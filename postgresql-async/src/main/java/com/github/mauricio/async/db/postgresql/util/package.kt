package com.github.mauricio.async.db.postgresql.util

/**
 * Contains package level aliases and type renames.
 */
package object util {

  /**
   * Alias to help compatibility.
   */
  @deprecated("Use com.github.mauricio.sql.db.postgresql.util.URLParser", since = "0.2.20")
  val ParserURL = URLParser

}