package com.github.jasync.sql.db.mysql

import com.github.jasync.sql.db.ConnectionPoolConfiguration
import com.github.jasync.sql.db.mysql.pool.MySQLConnectionFactory
import com.github.jasync.sql.db.mysql.util.URLParser
import com.github.jasync.sql.db.pool.ConnectionPool

object MySQLConnectionBuilder {


    @JvmStatic
    fun createConnectionPool(connectionPoolConfiguration: ConnectionPoolConfiguration): ConnectionPool<MySQLConnection> {
        return ConnectionPool(
                MySQLConnectionFactory(connectionPoolConfiguration.connectionConfiguration),
                connectionPoolConfiguration.poolConfiguration,
                connectionPoolConfiguration.executionContext
        )
    }

    @JvmStatic
    fun createConnectionPool(url: String,
                             configurator: ConnectionPoolConfiguration.() -> ConnectionPoolConfiguration = { this }): ConnectionPool<MySQLConnection> {
        val configuration = URLParser.parseOrDie(url)
        val connectionPoolConfiguration = with(configuration) {
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
            ).configurator()
        }
        return ConnectionPool(
                MySQLConnectionFactory(connectionPoolConfiguration.connectionConfiguration),
                connectionPoolConfiguration.poolConfiguration,
                connectionPoolConfiguration.executionContext
        )
    }
}