package com.github.jasync.sql.db.mysql.pool

import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.exceptions.ConnectionNotConnectedException
import com.github.jasync.sql.db.exceptions.ConnectionStillRunningQueryException
import com.github.jasync.sql.db.exceptions.ConnectionTimeoutedException
import com.github.jasync.sql.db.mysql.MySQLConnection
import com.github.jasync.sql.db.pool.ObjectFactory
import com.github.jasync.sql.db.util.ExecutorServiceUtils
import com.github.jasync.sql.db.util.NettyUtils
import com.github.jasync.sql.db.util.Try
import com.github.jasync.sql.db.util.map
import io.netty.channel.EventLoopGroup
import mu.KotlinLogging
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor


/**
 *
 * Connection pool factory for <<com.github.mauricio.sql.db.mysql.MySQLConnection>> objects.
 *
 * @param configuration a valid configuration to connect to a MySQL server.
 * @param group the netty event loop group - use this to select native/nio transport.
 * @param executionContext The thread pool to execute cpu tasks on.
 *
 */

open class MySQLConnectionFactory @JvmOverloads constructor(
    val configuration: Configuration,
    val group: EventLoopGroup = NettyUtils.DefaultEventLoopGroup,
    val executionContext: Executor = ExecutorServiceUtils.CommonPool
) : ObjectFactory<MySQLConnection> {

    /**
     *
     * Creates a valid object to be used in the pool. This method can block if necessary to make sure a correctly built
     * is created.
     *
     * @return
     */
    override fun create(): CompletableFuture<MySQLConnection> {
        val connection = MySQLConnection(configuration = configuration, group = group, executionContext = executionContext)
        return connection.connect()
    }

    /**
     *
     * This method should "close" and release all resources acquired by the pooled object. This object will not be used
     * anymore so any cleanup necessary to remove it from memory should be made in this method. Implementors should not
     * raise an exception under any circumstances, the factory should log and clean up the exception itself.
     *
     * @param item
     */
    override fun destroy(item: MySQLConnection) {
        try {
            item.disconnect()
        } catch (e: Exception) {
            log.error("Failed to close the connection", e)
        }
    }

    /**
     *
     * Validates that an object can still be used for it's purpose. This method should test the object to make sure
     * it's still valid for clients to use. If you have a database connection, test if you are still connected, if you're
     * accessing a file system, make sure you can still see and change the file.
     *
     * You decide how fast this method should return and what it will test, you should usually do something that's fast
     * enough not to slow down the pool usage, since this call will be made whenever an object returns to the pool.
     *
     * If this object is not valid anymore, a <<scala.util.Failure>> should be returned, otherwise <<scala.util.Success>>
     * should be the result of this call.
     *
     * @param item an object produced by this pool
     * @return
     */
    override fun validate(item: MySQLConnection): Try<MySQLConnection> {
        return Try {
            if (item.isTimeout()) {
                throw ConnectionTimeoutedException(item)
            }
            if (!item.isConnected()) {
                throw ConnectionNotConnectedException(item)
            }
            item.lastException()?.let { throw it }

            if (item.isQuerying()) {
                throw ConnectionStillRunningQueryException(item.count(), false)
            }

            item
        }
    }

    /**
     *
     * Does a full test on the given object making sure it's still valid. Different than validate, that's called whenever
     * an object is given back to the pool and should usually be fast, this method will be called when objects are
     * idle to make sure they don't "timeout" or become stale in anyway.
     *
     * For convenience, this method defaults to call **validate** but you can implement it in a different way if you
     * would like to.
     *
     * @param item an object produced by this pool
     * @return
     */
    override fun test(item: MySQLConnection): CompletableFuture<MySQLConnection> {
        return item.sendQuery("SELECT 0").map { item }
    }

}

private val log = KotlinLogging.logger {}
