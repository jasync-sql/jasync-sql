package com.github.jasync.sql.db.postgresql.pool

import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.pool.ConnectionFactory
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import mu.KotlinLogging
import java.util.concurrent.CompletableFuture

private val logger = KotlinLogging.logger {}

/**
 *
 * Object responsible for creating connection instances.
 *
 * @param configuration a valid configuration to connect to a PostgreSQL server.
 *
 */

class PostgreSQLConnectionFactory(val configuration: Configuration) : ConnectionFactory<PostgreSQLConnection>() {

    init {
        logger.debug { "PostgreSQLConnectionFactory created with configuration $configuration" }
    }

    override fun create(): CompletableFuture<PostgreSQLConnection> {
        val connection = PostgreSQLConnection(configuration)
        return connection.connect()
    }


}
