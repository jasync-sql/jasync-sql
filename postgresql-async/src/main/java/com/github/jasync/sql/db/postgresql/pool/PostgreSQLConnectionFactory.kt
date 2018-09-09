package com.github.jasync.sql.db.postgresql.pool

import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.exceptions.ConnectionTimeoutedException
import com.github.jasync.sql.db.pool.ObjectFactory
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import com.github.jasync.sql.db.util.ExecutorServiceUtils
import com.github.jasync.sql.db.util.Failure
import com.github.jasync.sql.db.util.NettyUtils
import com.github.jasync.sql.db.util.Success
import com.github.jasync.sql.db.util.Try
import io.netty.channel.EventLoopGroup
import mu.KotlinLogging
import java.nio.channels.ClosedChannelException
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

/**
 *
 * Object responsible for creating connection instances.
 *
 * @param configuration
 */

class PostgreSQLConnectionFactory(val configuration: Configuration,
                                  val group: EventLoopGroup = NettyUtils.DefaultEventLoopGroup,
                                  val executionContext: ExecutorService = ExecutorServiceUtils.CommonPool) : ObjectFactory<PostgreSQLConnection> {

    override fun create(): PostgreSQLConnection {
        val connection: PostgreSQLConnection = PostgreSQLConnection(configuration, group = group, executionContext = executionContext)
        connection.connect().get(configuration.connectTimeout.toMillis(), TimeUnit.MILLISECONDS)

        return connection
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

    override fun test(item: PostgreSQLConnection): Try<PostgreSQLConnection> {
        val result: Try<PostgreSQLConnection> = Try {
            item.sendQuery("SELECT 0").get(configuration.testTimeout.toMillis(), TimeUnit.MILLISECONDS)
            item
        }

        return when (result) {
            is Failure -> {
                try {
                    if (item.isConnected()) {
                        item.disconnect()
                    }
                } catch (e: Exception) {
                    logger.error("Failed disconnecting object", e)
                }
                result
            }
            is Success -> {
                result
            }
        }
    }

}
