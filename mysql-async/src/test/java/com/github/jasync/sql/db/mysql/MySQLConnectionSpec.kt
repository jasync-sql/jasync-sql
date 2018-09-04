package com.github.jasync.sql.db.mysql

import com.github.jasync.sql.db.Configuration
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MySQLConnectionSpec : ConnectionHelper() {

  @Test
  fun `connect to a MySQL instance with a password` () {
    val configuration = Configuration(
            "mysql_async",
            "localhost",
            port = ContainerHelper.getPort(),
            password = "root",
            database = "mysql_async_tests"
    )

    withNonConnectedConnection({
      connection ->
      assertEquals(connection, awaitFuture(connection.connect()))
    }, configuration)

  }

  @Test
  fun `connect to a MySQL instance without password` () {
    val configurationWithoutPassword = Configuration(
            "mysql_async_nopw",
            "localhost",
            port = ContainerHelper.getPort(),
            password = null,
            database = "mysql_async_tests"
    )
    withNonConnectedConnection({
      connection ->
      assertEquals(connection, awaitFuture(connection.connect()))
    }, configurationWithoutPassword)
  }

  @Test
  fun `connect to a MySQL instance without a database` () {

  val configurationWithoutDatabase = Configuration(
          "mysql_async_nopw",
          "localhost",
          port = ContainerHelper.getPort(),
          password = null,
          database = "mysql_async_tests"
  )

    withNonConnectedConnection({
      connection ->
      assertEquals(connection, awaitFuture(connection.connect()))
    }, configurationWithoutDatabase)
  }

  @Test
  fun `connect to a MySQL instance without database with password` () {
  val configurationWithPasswordWithoutDatabase = Configuration(
          "mysql_async",
          "localhost",
          port = ContainerHelper.getPort(),
          password = "root",
          database = null
  )
    withNonConnectedConnection({
      connection ->
      assertEquals(connection, awaitFuture(connection.connect()))
    }, configurationWithPasswordWithoutDatabase)
  }

  fun <T> withNonConnectedConnection(fn: (MySQLConnection) -> T, cfg: Configuration): T  {

    val connection = MySQLConnection (cfg)
    try {
     return fn(connection)
    } finally {
      if (connection.isConnected()) {
        connection.close().get(1, TimeUnit.SECONDS)
      }
    }
  }
}
