package com.github.aysnc.sql.db.integration.pool

import com.github.aysnc.sql.db.integration.ContainerHelper
import com.github.jasync.sql.db.ConnectionPoolConfiguration
import com.github.jasync.sql.db.pool.ConnectionPool
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import com.github.jasync.sql.db.postgresql.pool.PostgreSQLConnectionFactory

fun <R> withPool(fn: (ConnectionPool<PostgreSQLConnection>) -> R): R {
    val configuration = ConnectionPoolConfiguration(
        host = ContainerHelper.defaultConfiguration.host,
        port = ContainerHelper.defaultConfiguration.port,
        database = ContainerHelper.defaultConfiguration.database,
        username = ContainerHelper.defaultConfiguration.username,
        password = ContainerHelper.defaultConfiguration.password,
        maxActiveConnections = 10,
        maxIdleTime = 4,
        maxPendingQueries = 10
    )
    val pool = ConnectionPool(PostgreSQLConnectionFactory(ContainerHelper.defaultConfiguration), configuration)
    try {
        return fn(pool)
    } finally {
        pool.disconnect().get()
    }

}

fun <T> withPool(
    maxObjects: Int = 5,
    maxQueueSize: Int = 5,
    validationInterval: Long = 3000,
    fn: (ConnectionPool<PostgreSQLConnection>) -> T
): T {

    val poolConfiguration = ConnectionPoolConfiguration(
        host = ContainerHelper.defaultConfiguration.host,
        port = ContainerHelper.defaultConfiguration.port,
        database = ContainerHelper.defaultConfiguration.database,
        username = ContainerHelper.defaultConfiguration.username,
        password = ContainerHelper.defaultConfiguration.password,
        maxActiveConnections = maxObjects,
        maxIdleTime = 1000,
        maxPendingQueries = maxQueueSize,
        connectionValidationInterval = validationInterval
    )
    val factory = PostgreSQLConnectionFactory(ContainerHelper.defaultConfiguration)
    val pool = ConnectionPool<PostgreSQLConnection>(factory, poolConfiguration)

    try {
        return fn(pool)
    } finally {
        pool.disconnect().get()
    }

}