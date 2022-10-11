package com.github.jasync.r2dbc.mysql

import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.mysql.MySQLConnection.Companion.CLIENT_FOUND_ROWS_PROP_NAME
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
import mu.KotlinLogging
import java.time.Duration
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private val logger = KotlinLogging.logger {}

class MysqlConnectionFactoryProvider : ConnectionFactoryProvider {

    companion object {
        /**
         * Application name.
         */
        @JvmField
        val APPLICATION_NAME: Option<String> = Option.valueOf("applicationName")

        /**
         * Query timeout.
         */
        @JvmField
        val QUERY_TIMEOUT: Option<Duration> = Option.valueOf("queryTimeout")

        /**
         * Driver option value.
         */
        const val MYSQL_DRIVER = "mysql"

        var CLIENT_FOUND_ROWS: Boolean by ClientFoundRowsDelegate()

        init {
            // see issue https://github.com/jasync-sql/jasync-sql/issues/240
            CLIENT_FOUND_ROWS = true
        }

        class ClientFoundRowsDelegate : ReadWriteProperty<Companion, Boolean> {
            override fun getValue(thisRef: Companion, property: KProperty<*>): Boolean {
                return System.getProperty(CLIENT_FOUND_ROWS_PROP_NAME) != null
            }

            override fun setValue(thisRef: Companion, property: KProperty<*>, value: Boolean) {
                if (value) {
                    logger.info { "set $CLIENT_FOUND_ROWS_PROP_NAME=$value" }
                    System.setProperty(CLIENT_FOUND_ROWS_PROP_NAME, value.toString())
                } else {
                    logger.info { "remove $CLIENT_FOUND_ROWS_PROP_NAME" }
                    System.getProperties().remove(CLIENT_FOUND_ROWS_PROP_NAME)
                }
            }
        }
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    override fun create(connectionFactoryOptions: ConnectionFactoryOptions): JasyncConnectionFactory {
        val configuration = Configuration(
            host = connectionFactoryOptions.getValue(HOST) as String? ?: throw IllegalArgumentException("HOST is missing"),
            port = connectionFactoryOptions.getValue(PORT) as Int? ?: throw IllegalArgumentException("PORT is missing"),
            username = connectionFactoryOptions.getValue(USER) as String? ?: throw IllegalArgumentException("USER is missing"),
            password = connectionFactoryOptions.getValue(PASSWORD)?.toString(),
            database = connectionFactoryOptions.getValue(DATABASE) as String?,
            applicationName = connectionFactoryOptions.getValue(APPLICATION_NAME) as String?,
            connectionTimeout = (connectionFactoryOptions.getValue(CONNECT_TIMEOUT) as Duration?)?.toMillis()?.toInt() ?: 5000,
            queryTimeout = connectionFactoryOptions.getValue(QUERY_TIMEOUT) as Duration?
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
