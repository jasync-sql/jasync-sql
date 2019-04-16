package com.github.jasync.r2dbc.mysql

import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.mysql.pool.MySQLConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import io.r2dbc.spi.ConnectionFactoryOptions.CONNECT_TIMEOUT
import io.r2dbc.spi.ConnectionFactoryOptions.DATABASE
import io.r2dbc.spi.ConnectionFactoryOptions.DRIVER
import io.r2dbc.spi.ConnectionFactoryOptions.HOST
import io.r2dbc.spi.ConnectionFactoryOptions.PASSWORD
import io.r2dbc.spi.ConnectionFactoryOptions.PORT
import io.r2dbc.spi.ConnectionFactoryOptions.USER
import io.r2dbc.spi.ConnectionFactoryProvider
import io.r2dbc.spi.Option


class MysqlConnectionFactoryProvider : ConnectionFactoryProvider {

    companion object {
        /**
         * Application name.
         */
        @JvmField
        val APPLICATION_NAME: Option<String> = Option.valueOf("applicationName")

        /**
         * Driver option value.
         */
        const val MYSQL_DRIVER = "mysql"
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    override fun create(connectionFactoryOptions: ConnectionFactoryOptions): JasyncConnectionFactory {
        val configuration = Configuration(
            host = connectionFactoryOptions.getValue(HOST),
            port = connectionFactoryOptions.getValue(PORT),
            username = connectionFactoryOptions.getValue(USER),
            password = connectionFactoryOptions.getValue(PASSWORD)?.toString(),
            database = connectionFactoryOptions.getValue(DATABASE),
            applicationName = connectionFactoryOptions.getValue(APPLICATION_NAME),
            connectionTimeout = connectionFactoryOptions.getValue(CONNECT_TIMEOUT)?.toMillis()?.toInt() ?: 5000
        )
        return JasyncConnectionFactory(MySQLConnectionFactory(configuration))
    }

    override fun supports(connectionFactoryOptions: ConnectionFactoryOptions): Boolean {
        val driver = connectionFactoryOptions.getValue(DRIVER)
        return when {
            driver == null || driver != MYSQL_DRIVER -> false
            !connectionFactoryOptions.hasOption(HOST) -> false
            !connectionFactoryOptions.hasOption(PORT) -> false
            !connectionFactoryOptions.hasOption(USER) -> false
            else -> true
        }
    }

    override fun getDriver(): String = MYSQL_DRIVER
}
