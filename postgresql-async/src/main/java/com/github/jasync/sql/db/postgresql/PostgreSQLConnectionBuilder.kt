package com.github.jasync.sql.db.postgresql

import com.github.jasync.sql.db.ConnectionPoolConfiguration
import com.github.jasync.sql.db.pool.ConnectionPool
import com.github.jasync.sql.db.postgresql.pool.PostgreSQLConnectionFactory
import com.github.jasync.sql.db.postgresql.util.URLParser

object PostgreSQLConnectionBuilder {

    @JvmStatic
    fun createConnectionPool(connectionPoolConfiguration: ConnectionPoolConfiguration): ConnectionPool<PostgreSQLConnection> {
        return ConnectionPool(
                PostgreSQLConnectionFactory(connectionPoolConfiguration.connectionConfiguration),
                connectionPoolConfiguration.poolConfiguration,
                connectionPoolConfiguration.executionContext
        )
    }

    @JvmStatic
    fun createConnectionPool(url: String,
                             configurator: (ConnectionPoolConfiguration) -> ConnectionPoolConfiguration = { it }): ConnectionPool<PostgreSQLConnection> {
        val configuration = URLParser.parseOrDie(url)
        val connectionPoolConfiguration = configurator(with(configuration) {
            ConnectionPoolConfiguration(
                    username = username,
                    host = host,
                    port = port,
                    password = password,
                    database = database,
                    ssl = ssl,
                    charset = charset,
                    maximumMessageSize = maximumMessageSize,
                    allocator = allocator,
                    queryTimeout = queryTimeout?.toMillis()
            )
        })
        return ConnectionPool(
                PostgreSQLConnectionFactory(connectionPoolConfiguration.connectionConfiguration),
                connectionPoolConfiguration.poolConfiguration,
                connectionPoolConfiguration.executionContext
        )
    }

}

