package com.github.jasync.sql.db.postgresql.pool

import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.exceptions.ConnectionTimeoutedException
import com.github.jasync.sql.db.pool.ObjectFactory
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import com.github.jasync.sql.db.util.ExecutorServiceUtils
import com.github.jasync.sql.db.util.NettyUtils
import com.github.jasync.sql.db.util.Try
import com.github.jasync.sql.db.util.map
import com.github.jasync.sql.db.util.mapTry
import io.netty.channel.EventLoopGroup
import mu.KotlinLogging
import java.nio.channels.ClosedChannelException
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
) : ObjectFactory<PostgreSQLConnection> {

    override fun create(): CompletableFuture<PostgreSQLConnection> {
        val connection = PostgreSQLConnection(configuration, group = group, executionContext = executionContext)
        return connection.connect()
    }

    override fun destroy(item: PostgreSQLConnection) {
        item.disconnect()
    }

    /**
     *
     * Validates by checking if the connection is still connected to the database or not.
     *
     * @param item an object produced by this pool
     * @return
     */

    override fun validate(item: PostgreSQLConnection): Try<PostgreSQLConnection> {
        return Try {
            if (item.isTimeout()) {
                throw ConnectionTimeoutedException(item)
            }
            if (!item.isConnected() || item.hasRecentError()) {
                throw ClosedChannelException()
            }
            item.validateIfItIsReadyForQuery("Trying to give back a connection that is not ready for query")
            item
        }
    }

    /**
     *
     * Tests whether we can still send a **SELECT 0** statement to the database.
     *
     * @param item an object produced by this pool
     * @return
     */

    override fun test(item: PostgreSQLConnection): CompletableFuture<PostgreSQLConnection> {
        val result: CompletableFuture<PostgreSQLConnection> =
            item.sendQuery("SELECT 0").map { item }


        result.mapTry { c, t ->
            if (t != null) {
                try {
                    if (item.isConnected()) {
                        item.disconnect()
                    }
                } catch (e: Exception) {
                    logger.error("Failed disconnecting object", e)
                }
            }
        }
        return result
    }

}
