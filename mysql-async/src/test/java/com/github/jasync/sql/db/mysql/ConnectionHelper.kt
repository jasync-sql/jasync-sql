package com.github.jasync.sql.db.mysql

import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.ConnectionPoolConfiguration
import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.mysql.pool.MySQLConnectionFactory
import com.github.jasync.sql.db.pool.ConnectionPool
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 *
 * @author Doug Chimento dchimento@outbrain.com
 */
open class ConnectionHelper : ContainerHelper() {


    val createTableNumericColumns =
        """
      create temporary table numbers (
      id int auto_increment not null,
      number_tinyint tinyint not null,
      number_smallint smallint not null,
      number_mediumint mediumint not null,
      number_int int not null,
      number_bigint bigint not null,
      number_decimal decimal(9,6),
      number_float float,
      number_double double,
      primary key (id)
      )
    """

    val insertTableNumericColumns =
        """
      insert into numbers (
      number_tinyint,
      number_smallint,
      number_mediumint,
      number_int,
      number_bigint,
      number_decimal,
      number_float,
      number_double
      ) values
      (-100, 32766, 8388607, 2147483647, 9223372036854775807, 450.764491, 14.7, 87650.9876)
    """

    val preparedInsertTableNumericColumns =
        """
      insert into numbers (
      number_tinyint,
      number_smallint,
      number_mediumint,
      number_int,
      number_bigint,
      number_decimal,
      number_float,
      number_double
      ) values
      (?, ?, ?, ?, ?, ?, ?, ?)
    """

    val createTableTimeColumns =
        """CREATE TEMPORARY TABLE posts (
       id INT NOT NULL AUTO_INCREMENT,
       created_at_date DATE not null,
       created_at_datetime DATETIME not null,
       created_at_timestamp TIMESTAMP not null,
       created_at_time TIME not null,
       created_at_year YEAR not null,
       primary key (id)
      )"""

    val insertTableTimeColumns =
        """
      insert into posts (created_at_date, created_at_datetime, created_at_timestamp, created_at_time, created_at_year)
      values ( '2038-01-19', '2013-01-19 03:14:07', '2020-01-19 03:14:07', '03:14:07', '1999' )
    """
    val createTable = """CREATE TEMPORARY TABLE users (
                              id INT NOT NULL AUTO_INCREMENT ,
                              name VARCHAR(255) CHARACTER SET 'utf8' NOT NULL ,
                              PRIMARY KEY (id) );"""
    val insert = """INSERT INTO users (name) VALUES ('Boogie Man')"""
    val select = """SELECT * FROM users"""

    fun getConfiguration(): Configuration {
        return ContainerHelper.defaultConfiguration
    }

    fun <T> awaitFuture(f: CompletableFuture<T>): T {
        return f.get(5, TimeUnit.SECONDS)
    }

    fun <T> withPool(f: (ConnectionPool<MySQLConnection>) -> T): T {
        return withConfigurablePool(ContainerHelper.defaultConfiguration, f)
    }

    fun <T> withConfigurablePool(configuration: Configuration, f: (ConnectionPool<MySQLConnection>) -> T): T {

        val poolConfiguration = ConnectionPoolConfiguration(
            host = configuration.host,
            port = configuration.port,
            database = configuration.database,
            username = configuration.username,
            password = configuration.password,
            maxActiveConnections = 10,
            maxIdleTime = 4,
            maxPendingQueries = 10,
            queryTimeout = configuration.queryTimeout?.toMillis(),
            interceptors = configuration.interceptors
        )
        val factory = MySQLConnectionFactory(configuration)
        val pool = ConnectionPool(factory, poolConfiguration)

        try {
            return f(pool)
        } finally {
            awaitFuture(pool.disconnect())
        }
    }

    fun executeQuery(connection: Connection, query: String): QueryResult {
        return awaitFuture(connection.sendQuery(query))
    }

    fun <T> withConnection(fn: (MySQLConnection) -> T): T {
        return withConfigurableConnection(ContainerHelper.defaultConfiguration, fn)
    }

    fun <T> withConfigurableConnection(configuration: Configuration, fn: (MySQLConnection) -> T): T {
        val connection = MySQLConnection(configuration)

        try {
            awaitFuture(connection.connect())
            return fn(connection)
        } finally {
            awaitFuture(connection.close())
        }
    }

    fun <T> withConfigurableOpenConnection(configuration: Configuration, fn: (MySQLConnection) -> T): T {
        val connection = MySQLConnection(configuration)

        awaitFuture(connection.connect())
        return fn(connection)
    }

    fun executePreparedStatement(
        connection: Connection,
        query: String,
        values: List<Any?> = emptyList(),
        release: Boolean = false
    ): QueryResult {
        return awaitFuture(connection.sendPreparedStatement(query, values, release))
    }

    fun releasePreparedStatement(handler: MySQLConnection, query: String): Boolean {
        return awaitFuture(handler.releasePreparedStatement(query))
    }

}
