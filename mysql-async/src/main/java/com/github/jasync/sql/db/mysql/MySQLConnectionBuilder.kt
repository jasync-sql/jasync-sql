package com.github.jasync.sql.db.mysql

import com.github.jasync.sql.db.ConnectionPoolConfiguration
import com.github.jasync.sql.db.ConnectionPoolConfigurationBuilder
import com.github.jasync.sql.db.mysql.pool.MySQLConnectionFactory
import com.github.jasync.sql.db.mysql.util.URLParser
import com.github.jasync.sql.db.pool.ConnectionPool

object MySQLConnectionBuilder {


    @JvmStatic
    fun createConnectionPool(connectionPoolConfiguration: ConnectionPoolConfiguration): ConnectionPool<MySQLConnection> {
        return ConnectionPool(
            MySQLConnectionFactory(connectionPoolConfiguration.connectionConfiguration,
                connectionPoolConfiguration.eventLoopGroup,
                connectionPoolConfiguration.executionContext),
            connectionPoolConfiguration.poolConfiguration,
            connectionPoolConfiguration.executionContext
        )
    }

    @JvmStatic
    fun createConnectionPool(connectionPoolConfigurationBuilder: ConnectionPoolConfigurationBuilder): ConnectionPool<MySQLConnection> {
        return createConnectionPool(connectionPoolConfigurationBuilder.build())
    }

    @JvmStatic
    fun createConnectionPool(builder: ConnectionPoolConfigurationBuilder.() -> Unit): ConnectionPool<MySQLConnection> {
        return createConnectionPool(ConnectionPoolConfigurationBuilder().apply { builder() })
    }

    @JvmStatic
    fun createConnectionPool(
        url: String,
        configurator: ConnectionPoolConfigurationBuilder.() -> Unit = { }
    ): ConnectionPool<MySQLConnection> {
        val configuration = URLParser.parseOrDie(url)
        with(configuration) {
            val builder =
                ConnectionPoolConfigurationBuilder(
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
            builder.configurator()
            val connectionPoolConfiguration = builder.build()
            return ConnectionPool(
                MySQLConnectionFactory(connectionPoolConfiguration.connectionConfiguration,
                    connectionPoolConfiguration.eventLoopGroup,
                    connectionPoolConfiguration.executionContext),
                connectionPoolConfiguration.poolConfiguration,
                connectionPoolConfiguration.executionContext
            )
        }
    }
}
