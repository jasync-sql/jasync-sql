package com.github.mauricio.async.db.postgresql.util

import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.SSLConfiguration
import com.github.jasync.sql.db.util.AbstractURIParser
import java.net.URI
import java.nio.charset.Charset


/**
 * The PostgreSQL URL parser.
 */
object URLParser : AbstractURIParser() {
  override val DEFAULT = Configuration(
      username = "postgres",
      host = "localhost",
      port = 5432,
      password = null,
      database = null,
      ssl = SSLConfiguration()
  )

  override val SCHEME = "^postgres(?:ql)?$".toRegex()

  private val simplePGDB = "^postgresql:(\\w+)$".toRegex()

  override fun handleJDBC(uri: URI): Map<String, String> {
    return when {
      simplePGDB.matches(uri.schemeSpecificPart) -> mapOf(DBNAME to uri.schemeSpecificPart.removePrefix("postgresql:"))
      else -> parse(URI(uri.schemeSpecificPart))
    }
  }

  /**
   * Assembles a configuration out of the provided property map.  This is the generic form, subclasses may override to
   * handle additional properties.
   *
   * @param properties the extracted properties from the URL.
   * @param charset    the charset passed in to parse or parseOrDie.
   * @return
   */
  override fun assembleConfiguration(properties: Map<String, String>, charset: Charset): Configuration {
    // Add SSL Configuration
    return assembleConfiguration(properties, charset).copy(
        ssl = SSLConfiguration(properties)
    )
  }
}
