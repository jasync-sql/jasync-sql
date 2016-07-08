/**
 *
 */
package com.github.mauricio.async.db.postgresql.util

import java.net.URI
import java.nio.charset.Charset

import com.github.mauricio.async.db.{Configuration, SSLConfiguration}
import com.github.mauricio.async.db.util.AbstractURIParser

/**
 * The PostgreSQL URL parser.
 */
object URLParser extends AbstractURIParser {
  import AbstractURIParser._

  // Alias these for anyone still making use of them
  @deprecated("Use com.github.mauricio.async.db.AbstractURIParser.PORT", since = "0.2.20")
  val PGPORT = PORT

  @deprecated("Use com.github.mauricio.async.db.AbstractURIParser.DBNAME", since = "0.2.20")
  val PGDBNAME = DBNAME

  @deprecated("Use com.github.mauricio.async.db.AbstractURIParser.HOST", since = "0.2.20")
  val PGHOST = HOST

  @deprecated("Use com.github.mauricio.async.db.AbstractURIParser.USERNAME", since = "0.2.20")
  val PGUSERNAME = USERNAME

  @deprecated("Use com.github.mauricio.async.db.AbstractURIParser.PASSWORD", since = "0.2.20")
  val PGPASSWORD = PASSWORD

  @deprecated("Use com.github.mauricio.async.db.postgresql.util.URLParser.DEFAULT.port", since = "0.2.20")
  val DEFAULT_PORT = "5432"

  /**
   * The default configuration for PostgreSQL.
   */
  override val DEFAULT = Configuration(
    username = "postgres",
    host = "localhost",
    port = 5432,
    password = None,
    database = None,
    ssl = SSLConfiguration()
  )

  override protected val SCHEME = "^postgres(?:ql)?$".r

  private val simplePGDB = "^postgresql:(\\w+)$".r

  override protected def handleJDBC(uri: URI): Map[String, String] = uri.getSchemeSpecificPart match {
    case simplePGDB(db) => Map(DBNAME -> db)
    case x => parse(new URI(x))
  }

  /**
   * Assembles a configuration out of the provided property map.  This is the generic form, subclasses may override to
   * handle additional properties.
   *
   * @param properties the extracted properties from the URL.
   * @param charset    the charset passed in to parse or parseOrDie.
   * @return
   */
  override protected def assembleConfiguration(properties: Map[String, String], charset: Charset): Configuration = {
    // Add SSL Configuration
    super.assembleConfiguration(properties, charset).copy(
      ssl = SSLConfiguration(properties)
    )
  }
}
