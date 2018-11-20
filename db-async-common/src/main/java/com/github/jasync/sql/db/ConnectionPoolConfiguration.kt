package com.github.jasync.sql.db

import com.github.jasync.sql.db.pool.PoolConfiguration
import com.github.jasync.sql.db.util.ExecutorServiceUtils
import com.github.jasync.sql.db.util.nullableMap
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.PooledByteBufAllocator
import io.netty.util.CharsetUtil
import java.nio.charset.Charset
import java.time.Duration
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

/**
 *
 * Contains the configuration necessary to connect to a database.
 *
 * @param host database host, defaults to "localhost"
 * @param port database port, defaults to 5432
 * @param database database name, defaults to no database
 * @param username database username
 * @param password password, defaults to no password
 * @param ssl ssl configuration
 * @param charset charset for the connection, defaults to UTF-8, make sure you know what you are doing if you
 *                change this
 * @param maximumMessageSize the maximum size a message from the server could possibly have, this limits possible
 *                           OOM or eternal loop attacks the client could have, defaults to 16 MB. You can set this
 *                           to any value you would like but again, make sure you know what you are doing if you do
 *                           change it.
 * @param allocator the netty buffer allocator to be used
 *  * @param maxObjects how many objects this pool will hold
 * @param maxIdleTime number of milliseconds for which the objects are going to be kept as idle (not in use by clients of the pool)
 * @param maxPendingQueries when there are no more objects, the pool can queue up requests to serve later then there
 *                     are objects available, this is the maximum number of enqueued requests
 * @param connectionValidationInterval pools will use this value as the timer period to validate idle objects.
 * @param connectionCreateTimeout the timeout for connecting to servers
 * @param connectionTestTimeout the timeout for connection tests performed by pools
 * @param queryTimeout the optional query timeout
 * @param executionContext the thread pool to run the callbacks on
 *
 */
data class ConnectionPoolConfiguration @JvmOverloads constructor(
        val host: String = "localhost",
        val port: Int = 5432,
        val database: String? = null,
        val username: String = "dbuser",
        val password: String? = null,
        val maxActiveConnections: Int = 1,
        val maxIdleTime: Long = TimeUnit.MINUTES.toMillis(1),
        val maxPendingQueries: Int = Int.MAX_VALUE,
        val connectionValidationInterval: Long = 5000,
        val connectionCreateTimeout: Long = 5000,
        val connectionTestTimeout: Long = 5000,
        val queryTimeout: Long? = null,
        val executionContext: Executor = ExecutorServiceUtils.CommonPool,
        val ssl: SSLConfiguration = SSLConfiguration(),
        val charset: Charset = CharsetUtil.UTF_8,
        val maximumMessageSize: Int = 16777216,
        val allocator: ByteBufAllocator = PooledByteBufAllocator.DEFAULT
) {
    val connectionConfiguration = Configuration(
            username = username,
            host = host,
            port = port,
            password = password,
            database = database,
            ssl = ssl,
            charset = charset,
            maximumMessageSize = maximumMessageSize,
            allocator = allocator,
            queryTimeout = queryTimeout.nullableMap { Duration.ofMillis(it) }
    )

    val poolConfiguration = PoolConfiguration(
            maxObjects = maxActiveConnections,
            maxIdle = maxIdleTime,
            maxQueueSize = maxPendingQueries,
            validationInterval = connectionValidationInterval,
            createTimeout = connectionCreateTimeout,
            testTimeout = connectionTestTimeout,
            queryTimeout = queryTimeout
    )


}
