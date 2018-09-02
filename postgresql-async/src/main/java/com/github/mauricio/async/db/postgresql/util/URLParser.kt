package com.github.mauricio.async.db.postgresql.util

import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.SSLConfiguration
import com.github.jasync.sql.db.util.AbstractURIParser.Companion.DBNAME
import com.github.jasync.sql.db.util.AbstractURIParser.Companion.HOST
import com.github.jasync.sql.db.util.AbstractURIParser.Companion.PASSWORD
import com.github.jasync.sql.db.util.AbstractURIParser.Companion.PORT
import com.github.jasync.sql.db.util.AbstractURIParser.Companion.USERNAME
import com.github.mauricio.async.db.postgresql.util.ArrayStreamingParser.parse
import java.net.URI
import java.nio.charset.Charset

// Alias these for anyone still making use of them
@Deprecated("Use com.github.mauricio.sql.db.AbstractURIParser.PORT, since = 0.2.20")
val PGPORT = PORT

@Deprecated("Use com.github.mauricio.sql.db.AbstractURIParser.DBNAME, since = 0.2.20")
val PGDBNAME = DBNAME

@Deprecated("Use com.github.mauricio.sql.db.AbstractURIParser.HOST, since = 0.2.20")
val PGHOST = HOST

@Deprecated("Use com.github.mauricio.sql.db.AbstractURIParser.USERNAME, since = 0.2.20")
val PGUSERNAME = USERNAME

@Deprecated("Use com.github.mauricio.sql.db.AbstractURIParser.PASSWORD, since = 0.2.20")
val PGPASSWORD = PASSWORD

@Deprecated("Use com.github.mauricio.sql.db.postgresql.util.URLParser.DEFAULT.port, since = 0.2.20")
val DEFAULT_PORT = "5432"

/**
 * The funault configuration for PostgreSQL.
 */
val DEFAULT = Configuration(
    username = "postgres",
    host = "localhost",
    port = 5432,
    password = null,
    database = null,
    ssl = SSLConfiguration()
)

val SCHEME = "^postgres(?:ql)?$".r

private val simplePGDB = "^postgresql:(\\w+)$".r

fun handleJDBC(uri: URI): Map<String, String> {
  return when (uri.schemeSpecificPart) {
    simplePGDB(db) -> mapOf(DBNAME to db)
    x -> parse(URI(x))
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
protected fun assembleConfiguration(properties: Map<String, String>, charset: Charset): Configuration {
  // Add SSL Configuration
  assembleConfiguration(properties, charset).copy(
      ssl = SSLConfiguration(properties)
  )
}