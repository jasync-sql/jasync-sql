package com.github.jasync.sql.db.mysql

import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.mysql.codec.MySQLHandlerDelegate
import com.github.jasync.sql.db.mysql.message.server.OkMessage
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import org.assertj.core.api.Assertions
import org.junit.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException

class MySQLConnectionSpec : ConnectionHelper() {

    @Test
    fun `connect should return with timeout exception after create timeout`() {

        class MySQLSlowConnectionDelegate(
            private val delegate: MySQLHandlerDelegate,
            private val onOkSlowdownInMillis: Int
        ) : MySQLHandlerDelegate by delegate {
            override fun onOk(message: OkMessage) {
                Thread.sleep(onOkSlowdownInMillis.toLong())
                delegate.onOk(message)
            }
        }

        val configuration = Configuration(
            "mysql_async",
            "localhost",
            port = ContainerHelper.getPort(),
            password = "root",
            database = "mysql_async_tests",
            connectionTimeout = 10
        )

        val connection: CompletableFuture<out Connection> = MySQLConnection(
            configuration,
            withDelegate = { delegate ->
                MySQLSlowConnectionDelegate(
                    delegate,
                    configuration.connectionTimeout * 2
                )
            },
        ).connect()

        verifyException(ExecutionException::class.java, TimeoutException::class.java) {
            awaitFuture(connection)
        }
    }

    @Test
    fun `connect to a MySQL instance with a password`() {
        val configuration = Configuration(
            "mysql_async",
            "localhost",
            port = ContainerHelper.getPort(),
            password = "root",
            database = "mysql_async_tests"
        )

        withNonConnectedConnection({ connection ->
            assertEquals(connection, awaitFuture(connection.connect()))
        }, configuration)
    }

    @Test
    fun `connect to a MySQL instance without password`() {
        val configurationWithoutPassword = Configuration(
            "mysql_async_nopw",
            "localhost",
            port = ContainerHelper.getPort(),
            password = null,
            database = "mysql_async_tests"
        )
        withNonConnectedConnection({ connection ->
            assertEquals(connection, awaitFuture(connection.connect()))
        }, configurationWithoutPassword)
    }

    @Test
    fun `connect to a MySQL instance without a database`() {

        val configurationWithoutDatabase = Configuration(
            "mysql_async_nopw",
            "localhost",
            port = ContainerHelper.getPort(),
            password = null,
            database = "mysql_async_tests"
        )

        withNonConnectedConnection({ connection ->
            assertEquals(connection, awaitFuture(connection.connect()))
        }, configurationWithoutDatabase)
    }

    @Test
    fun `connect to a MySQL instance without database with password`() {
        val configurationWithPasswordWithoutDatabase = Configuration(
            "mysql_async",
            "localhost",
            port = ContainerHelper.getPort(),
            password = "root",
            database = null
        )
        withNonConnectedConnection({ connection ->
            assertEquals(connection, awaitFuture(connection.connect()))
        }, configurationWithPasswordWithoutDatabase)
    }

    fun <T> withNonConnectedConnection(fn: (MySQLConnection) -> T, cfg: Configuration): T {
        val connection = MySQLConnection(cfg)
        try {
            return fn(connection)
        } finally {
            if (connection.isConnected()) {
                connection.close().get(1, TimeUnit.SECONDS)
            }
        }
    }

    @Test
    fun `connect to a MySQL instance with _client_name`() {
        val configurationWithAppName = Configuration(
                "mysql_async",
                "localhost",
                port = ContainerHelper.getPort(),
                password = "root",
                database = "mysql_async_tests",
                applicationName = "jasync_test"
        )
        withConfigurableConnection(configurationWithAppName) { connection ->
            val result = executeQuery(connection, "SELECT ATTR_VALUE FROM performance_schema.session_connect_attrs WHERE processlist_id = CONNECTION_ID() and ATTR_NAME='_client_name'")
            Assertions.assertThat(result.rowsAffected).isEqualTo(1)
            Assertions.assertThat(result.rows[0].getString(0)).isEqualTo("jasync_test")
        }
    }
}
