package com.github.jasync.sql.db

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ConnectionPoolConfigurationTest {

    @Test
    fun `simple test`() {
        val configuration: ConnectionPoolConfiguration = ConnectionPoolConfigurationBuilder().build()
        assertThat(configuration.host).isEqualTo("localhost")
        assertThat(configuration.connectionConfiguration.host).isEqualTo("localhost")
    }

    @Test
    fun `test all fields`() {
        val configuration: ConnectionPoolConfiguration = ConnectionPoolConfigurationBuilder(
            host = "host",
            port = 1,
            database = "database",
            username = "username",
            password = "password",
            maxActiveConnections = 10,
            maxIdleTime = 11,
            maxPendingQueries = 12,
            connectionValidationInterval = 13,
            connectionCreateTimeout = 14,
            connectionTestTimeout = 15,
            queryTimeout = 16,
            maximumMessageSize = 17,
            applicationName = "applicationName",
            maxConnectionTtl = 18,
            minIdleConnections = 5
        ).build()
        assertThat(configuration.host).isEqualTo("host")
        assertThat(configuration.connectionConfiguration.host).isEqualTo("host")
        assertThat(configuration.connectionConfiguration.port).isEqualTo(1)
        assertThat(configuration.connectionConfiguration.database).isEqualTo("database")
        assertThat(configuration.connectionConfiguration.username).isEqualTo("username")
        assertThat(configuration.connectionConfiguration.password).isEqualTo("password")
        assertThat(configuration.connectionConfiguration.maximumMessageSize).isEqualTo(17)
        assertThat(configuration.connectionConfiguration.applicationName).isEqualTo("applicationName")
        assertThat(configuration.maxActiveConnections).isEqualTo(10)
        assertThat(configuration.poolConfiguration.maxObjects).isEqualTo(10)
        assertThat(configuration.poolConfiguration.maxIdle).isEqualTo(11)
        assertThat(configuration.poolConfiguration.maxObjectTtl).isEqualTo(18)
        assertThat(configuration.maxPendingQueries).isEqualTo(12)
        assertThat(configuration.poolConfiguration.maxQueueSize).isEqualTo(12)
        assertThat(configuration.connectionValidationInterval).isEqualTo(13)
        assertThat(configuration.poolConfiguration.validationInterval).isEqualTo(13)
        assertThat(configuration.connectionCreateTimeout).isEqualTo(14)
        assertThat(configuration.poolConfiguration.createTimeout).isEqualTo(28)
        assertThat(configuration.connectionTestTimeout).isEqualTo(15)
        assertThat(configuration.poolConfiguration.testTimeout).isEqualTo(15)
        assertThat(configuration.poolConfiguration.queryTimeout).isEqualTo(16)
        assertThat(configuration.minIdleConnections).isEqualTo(5)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test error port`() {
        ConnectionPoolConfigurationBuilder(
            port = -1
        ).build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test error maximumMessageSize`() {
        ConnectionPoolConfigurationBuilder(
            maximumMessageSize = -1
        ).build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test error maxActiveConnections`() {
        ConnectionPoolConfigurationBuilder(
            maxActiveConnections = -1
        ).build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test error maxIdleTime`() {
        ConnectionPoolConfigurationBuilder(
            maxIdleTime = -1
        ).build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test error maxPendingQueries`() {
        ConnectionPoolConfigurationBuilder(
            maxPendingQueries = -1
        ).build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test error connectionValidationInterval`() {
        ConnectionPoolConfigurationBuilder(
            connectionValidationInterval = -1
        ).build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test error connectionCreateTimeout`() {
        ConnectionPoolConfigurationBuilder(
            connectionCreateTimeout = -1
        ).build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test error connectionTestTimeout`() {
        ConnectionPoolConfigurationBuilder(
            queryTimeout = -1
        ).build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test error minIdleConnections`() {
        ConnectionPoolConfigurationBuilder(
            minIdleConnections = -1
        ).build()
    }
}
