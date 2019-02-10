package com.github.jasync.sql.db.postgresql.pool

import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.pool.ConnectionFactory
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import com.github.jasync.sql.db.util.ExecutorServiceUtils
import com.github.jasync.sql.db.util.NettyUtils
import io.netty.channel.EventLoopGroup
import mu.KotlinLogging
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

private val logger = KotlinLogging.logger {}

/**
 *
 * Object responsible for creating connection instances.
 *
 * @param configuration a valid configuration to connect to a PostgreSQL server.
 * @param group the netty event loop group - use this to select native/nio transport.
 * @param executionContext The thread pool to execute cpu tasks on.
 *
 */

class PostgreSQLConnectionFactory @JvmOverloads constructor(
    val configuration: Configuration,
    val group: EventLoopGroup = NettyUtils.DefaultEventLoopGroup,
    val executionContext: Executor = ExecutorServiceUtils.CommonPool
) : ConnectionFactory<PostgreSQLConnection>() {

    override fun create(): CompletableFuture<PostgreSQLConnection> {
        val connection = PostgreSQLConnection(configuration, group = group, executionContext = executionContext)
        return connection.connect()
    }



}
