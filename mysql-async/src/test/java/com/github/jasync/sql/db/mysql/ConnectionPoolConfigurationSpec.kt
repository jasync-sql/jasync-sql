package com.github.jasync.sql.db.mysql

import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.ConnectionPoolConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ConnectionPoolConfigurationSpec : ConnectionHelper() {

    @Test
    fun `configured connection pool should be able to run a query`() {
        withPoolConfigurationConnectionConnection { connection ->
            assertThat(executeQuery(connection, this.createTable).rowsAffected).isEqualTo(0)
        }
    }

    private fun <T> withPoolConfigurationConnectionConnection(fn: (Connection) -> T): T {
        val connection = MySQLConnectionBuilder.createConnectionPool(
            ConnectionPoolConfiguration(
                host = ContainerHelper.defaultConfiguration.host,
                port = ContainerHelper.defaultConfiguration.port,
                database = ContainerHelper.defaultConfiguration.database,
                username = ContainerHelper.defaultConfiguration.username,
                password = ContainerHelper.defaultConfiguration.password
            )
        )
        try {
            return fn(connection)
        } finally {
            awaitFuture(connection.disconnect())
        }
    }

    @Test
    fun `configured connection pool with a builder should be able to run a query`() {
        withPoolConfigurationConnectionBuilderConnection { connection ->
            assertThat(executeQuery(connection, this.createTable).rowsAffected).isEqualTo(0)
        }
    }

    private fun <T> withPoolConfigurationConnectionBuilderConnection(fn: (Connection) -> T): T {
        val connection = MySQLConnectionBuilder.createConnectionPool {
            host = ContainerHelper.defaultConfiguration.host
            port = ContainerHelper.defaultConfiguration.port
            database = ContainerHelper.defaultConfiguration.database
            username = ContainerHelper.defaultConfiguration.username
            password = ContainerHelper.defaultConfiguration.password
        }
        try {
            return fn(connection)
        } finally {
            awaitFuture(connection.disconnect())
        }
    }

    @Test
    fun `url configured connection pool should be able to run a query`() {
        withPoolUrlConfigurationConnection { connection ->
            assertThat(executeQuery(connection, this.createTable).rowsAffected).isEqualTo(0)
        }
    }

    private fun <T> withPoolUrlConfigurationConnection(fn: (Connection) -> T): T {
        val connectionUri = with(ContainerHelper.defaultConfiguration) {
            "jdbc:mysql://$host:$port/$database?user=$username&password=$password"
        }

        val connection = MySQLConnectionBuilder.createConnectionPool(connectionUri) {
            connectionCreateTimeout = 1
        }
        assertThat(connection.configuration.connectionCreateTimeout).isEqualTo(1)
        try {
            return fn(connection)
        } finally {
            awaitFuture(connection.disconnect())
        }
    }


}


