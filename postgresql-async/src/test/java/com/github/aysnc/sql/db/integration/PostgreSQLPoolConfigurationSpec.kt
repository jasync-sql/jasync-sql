package com.github.aysnc.sql.db.integration

import com.github.aysnc.sql.db.integration.ContainerHelper.defaultConfiguration
import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.ConnectionPoolConfiguration
import com.github.jasync.sql.db.ConnectionPoolConfigurationBuilder
import com.github.jasync.sql.db.postgresql.PostgreSQLConnectionBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test


class PostgreSQLPoolConfigurationSpec : DatabaseTestHelper() {


    val create = """create temp table type_test_table (
            bigserial_column bigserial not null,
            smallint_column smallint not null,
            integer_column integer not null,
            decimal_column decimal(10,4),
            real_column real,
            double_column double precision,
            serial_column serial not null,
            varchar_column varchar(255),
            text_column text,
            timestamp_column timestamp,
            date_column date,
            time_column time,
            boolean_column boolean,
            constraint bigserial_column_pkey primary key (bigserial_column)
          ) with oids"""

    @Test
    fun `"handler" should     "create a table in the database" with connection pool`() {

        withPoolConfigurationConnectionConnection { handler ->
            assertThat(executeDdl(handler, this.create)).isEqualTo(0)
        }

    }

    private fun <T> withPoolConfigurationConnectionConnection(fn: (Connection) -> T): T {
        val connection = PostgreSQLConnectionBuilder.createConnectionPool(
            ConnectionPoolConfiguration(
                host = defaultConfiguration.host,
                port = defaultConfiguration.port,
                database = defaultConfiguration.database,
                username = defaultConfiguration.username,
                password = defaultConfiguration.password
            )
        )
        try {
//            awaitFuture(connection.connect())
            return fn(connection)
        } finally {
            awaitFuture(connection.close())
        }
    }

    @Test
    fun `"handler" should     "create a table in the database" with connection pool builder`() {

        withPoolConfigurationConnectionBuilderConnection { handler ->
            assertThat(executeDdl(handler, this.create)).isEqualTo(0)
        }

    }

    private fun <T> withPoolConfigurationConnectionBuilderConnection(fn: (Connection) -> T): T {
        val connection = PostgreSQLConnectionBuilder.createConnectionPool(
            ConnectionPoolConfigurationBuilder(
                host = defaultConfiguration.host,
                port = defaultConfiguration.port,
                database = defaultConfiguration.database,
                username = defaultConfiguration.username,
                password = defaultConfiguration.password
            )
        )
        try {
//            awaitFuture(connection.connect())
            return fn(connection)
        } finally {
            awaitFuture(connection.close())
        }
    }

    @Test
    fun `"handler" should     "create a table in the database" with connection pool parsed from url`() {

        withPoolUrlConfigurationConnection { handler ->
            assertThat(executeDdl(handler, this.create)).isEqualTo(0)
        }

    }

    private fun <T> withPoolUrlConfigurationConnection(fn: (Connection) -> T): T {
        val connectionUri = with(ContainerHelper.defaultConfiguration) {
            "jdbc:postgresql://$host:$port/$database?user=$username&password=$password"
        }

        val connection = PostgreSQLConnectionBuilder.createConnectionPool(connectionUri) {
            connectionCreateTimeout = 1
        }
        assertThat(connection.configuration.createTimeout).isEqualTo(1)
        try {
//            awaitFuture(connection.connect())
            return fn(connection)
        } finally {
            awaitFuture(connection.close())
        }
    }

}
