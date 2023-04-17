package com.github.jasync.r2dbc.mysql.integ

import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.ConnectionPoolConfiguration
import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.SSLConfiguration
import com.github.jasync.sql.db.mysql.MySQLConnection
import com.github.jasync.sql.db.mysql.pool.MySQLConnectionFactory
import com.github.jasync.sql.db.pool.ConnectionPool
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 *
 * @author Doug Chimento dchimento@outbrain.com
 */
open class R2dbcConnectionHelper : R2dbcContainerHelper() {

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
       created_at_datetime DATETIME(6) not null,
       created_at_timestamp TIMESTAMP(6) not null,
       created_at_time TIME not null,
       created_at_year YEAR not null,
       primary key (id)
      )"""

    val insertTableTimeColumns =
        """
      insert into posts (created_at_date, created_at_datetime, created_at_timestamp, created_at_time, created_at_year)
      values ( '2038-01-19', '2013-01-19 03:14:07', '2020-01-19 03:14:07', '03:14:07', '1999' )
    """

    val createUserTable = """CREATE TEMPORARY TABLE users (
                              id INT NOT NULL AUTO_INCREMENT ,
                              name VARCHAR(255) CHARACTER SET 'utf8' NOT NULL ,
                              PRIMARY KEY (id) );"""
    val insertUsers = """INSERT INTO users (name) VALUES ('Boogie Man'), ('Dambeldor')"""
    val selectUsers = """SELECT * FROM users"""

    val createPostTable = """CREATE TEMPORARY TABLE posts (
                              id INT NOT NULL AUTO_INCREMENT ,
                              title VARCHAR(255) CHARACTER SET 'utf8' NOT NULL ,
                              user_id INT NOT NULL ,
                              PRIMARY KEY (id) );"""
    val insertPosts = """INSERT INTO posts (title, user_id) VALUES ('Hello World', 1), ('Hello World 2', 2)"""

    fun getConfiguration(): Configuration {
        return defaultConfiguration
    }

    fun <T> awaitFuture(f: CompletableFuture<T>): T {
        return f.get(10, TimeUnit.SECONDS)
    }

    fun <T> withPool(f: (ConnectionPool<MySQLConnection>) -> T): T {
        return withConfigurablePool(defaultConfiguration, f)
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
        return withConfigurableConnection(defaultConfiguration, fn)
    }

    fun <T> withSSLConnection(
        host: String = "localhost",
        sslConfig: SSLConfiguration,
        fn: (MySQLConnection) -> T
    ): T {
        val config = defaultConfiguration.copy(
            host = host,
            ssl = sslConfig
        )
        return withConfigurableConnection(config, fn)
    }

    fun <T> withConfigurableConnection(configuration: Configuration, fn: (MySQLConnection) -> T): T {
        val connection = MySQLConnection(configuration)
        awaitFuture(connection.connect())
        val res = fn(connection)
        awaitFuture(connection.close())
        return res
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
