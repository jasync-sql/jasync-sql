package com.github.jasync.sql.db

import com.github.jasync.sql.db.interceptor.QueryInterceptor
import com.github.jasync.sql.db.pool.PoolConfiguration
import com.github.jasync.sql.db.util.ExecutorServiceUtils
import com.github.jasync.sql.db.util.NettyUtils
import com.github.jasync.sql.db.util.nullableMap
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.EventLoopGroup
import io.netty.util.CharsetUtil
import java.nio.charset.Charset
import java.time.Duration
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.function.Supplier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 *
 * Contains the configuration necessary to connect to a database.
 *
 * @param host database host, defaults to "localhost"
 * @param port database port, defaults to 5432
 * @param database database name, defaults to no database
 * @param username database username
 * @param password password, defaults to no password
 * @param maxActiveConnections how many conncetions this pool will keep live
 * @param maxIdleTime number of milliseconds for which the objects are going to be kept as idle (not in use by clients of the pool)
 * @param maxPendingQueries when there are no more connections, the pool can queue up requests to serve later then there
 *                     are connections available, this is the maximum number of enqueued requests
 * @param connectionValidationInterval pools will use this value as the timer period to validate idle objects.
 * @param connectionCreateTimeout the timeout for connecting to servers
 * @param connectionTestTimeout the timeout for connection tests performed by pools
 * @param queryTimeout the optional query timeout
 * @param eventLoopGroup the netty event loop group - use this to select native/nio transport.
 * @param executionContext the thread pool to run the callbacks on
 * @param coroutineDispatcher thread pool for the actor operations of the connection pool
 * @param ssl ssl configuration
 * @param charset charset for the connection, defaults to UTF-8, make sure you know what you are doing if you
 *                change this
 * @param maximumMessageSize the maximum size a message from the server could possibly have, this limits possible
 *                           OOM or eternal loop attacks the client could have, defaults to 16 MB. You can set this
 *                           to any value you would like but again, make sure you know what you are doing if you do
 *                           change it.
 * @param allocator the netty buffer allocator to be used
 * @param applicationName optional name to be passed to the database for reporting
 * @param interceptors optional delegates to call on query execution
 * @param maxConnectionTtl number of milliseconds an object in this pool should be kept alive, negative values mean no aging out
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
    val eventLoopGroup: EventLoopGroup = NettyUtils.DefaultEventLoopGroup,
    val executionContext: Executor = ExecutorServiceUtils.CommonPool,
    val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
    val ssl: SSLConfiguration = SSLConfiguration(),
    val charset: Charset = CharsetUtil.UTF_8,
    val maximumMessageSize: Int = 16777216,
    val allocator: ByteBufAllocator = PooledByteBufAllocator.DEFAULT,
    val applicationName: String? = null,
    val interceptors: List<Supplier<QueryInterceptor>> = emptyList(),
    val maxConnectionTtl: Long? = null

) {
    init {
        require(port > 0) { "port should be positive: $port" }
        require(maximumMessageSize > 0) { "maximumMessageSize should be positive: $maximumMessageSize" }
        require(maxActiveConnections > 0) { "maxActiveConnections should be positive: $maxActiveConnections" }
        require(maxIdleTime >= 0) { "maxIdleTime should not be negative: $maxIdleTime" }
        require(maxPendingQueries >= 0) { "maxPendingQueries should not be negative: $maxPendingQueries" }
        require(connectionValidationInterval >= 0) { "connectionValidationInterval should not be negative: $connectionValidationInterval" }
        require(connectionCreateTimeout >= 0) { "connectionCreateTimeout should not be negative: $connectionCreateTimeout" }
        require(connectionTestTimeout >= 0) { "connectionTestTimeout should not be negative: $connectionTestTimeout" }
        queryTimeout?.let { require(it >= 0) { "queryTimeout should not be negative: $it" } }
        maxConnectionTtl?.let { require(it >= 0) { "queryTimeout should not be negative: $it" } }
    }

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
        connectionTimeout = connectionCreateTimeout.toInt(),
        queryTimeout = queryTimeout.nullableMap { Duration.ofMillis(it) },
        applicationName = applicationName,
        interceptors = interceptors,
        executionContext = executionContext,
        eventLoopGroup = eventLoopGroup
    )

    val poolConfiguration = PoolConfiguration(
        maxObjects = maxActiveConnections,
        maxIdle = maxIdleTime,
        maxObjectTtl = maxConnectionTtl,
        maxQueueSize = maxPendingQueries,
        validationInterval = connectionValidationInterval,
        createTimeout = connectionCreateTimeout,
        testTimeout = connectionTestTimeout,
        queryTimeout = queryTimeout,
        coroutineDispatcher = coroutineDispatcher
    )

    override fun toString() = """ConnectionPoolConfiguration(host=$host, port=REDACTED, 
|database=$database,username=REDACTED, password=REDACTED, 
|maxActiveConnections=$maxActiveConnections, 
|maxIdleTime=$maxIdleTime, 
|maxPendingQueries=$maxPendingQueries, 
|connectionValidationInterval=$connectionValidationInterval, 
|connectionCreateTimeout=$connectionCreateTimeout, 
|connectionTestTimeout=$connectionTestTimeout, 
|queryTimeout=$queryTimeout,
|ssl=$ssl, 
|charset=$charset, 
|maximumMessageSize=$maximumMessageSize, 
|allocator=$allocator, 
|applicationName=$applicationName, 
|interceptors=$interceptors, maxConnectionTtl=$maxConnectionTtl)""".trimMargin()
}

/**
 * This is a builder class for ConnectionPoolConfiguration
 * It has the same parameters but with var instead of val so they can be altered
 * build() method will build the actual ConnectionPoolConfiguration that is used
 */
data class ConnectionPoolConfigurationBuilder @JvmOverloads constructor(
    var host: String = "localhost",
    var port: Int = 5432,
    var database: String? = null,
    var username: String = "dbuser",
    var password: String? = null,
    var maxActiveConnections: Int = 1,
    var maxIdleTime: Long = TimeUnit.MINUTES.toMillis(1),
    var maxPendingQueries: Int = Int.MAX_VALUE,
    var connectionValidationInterval: Long = 5000,
    var connectionCreateTimeout: Long = 5000,
    var connectionTestTimeout: Long = 5000,
    var queryTimeout: Long? = null,
    var executionContext: Executor = ExecutorServiceUtils.CommonPool,
    val eventLoopGroup: EventLoopGroup = NettyUtils.DefaultEventLoopGroup,
    val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
    var ssl: SSLConfiguration = SSLConfiguration(),
    var charset: Charset = CharsetUtil.UTF_8,
    var maximumMessageSize: Int = 16777216,
    var allocator: ByteBufAllocator = PooledByteBufAllocator.DEFAULT,
    var applicationName: String? = null,
    var interceptors: MutableList<Supplier<QueryInterceptor>> = mutableListOf<Supplier<QueryInterceptor>>(),
    var maxConnectionTtl: Long? = null
) {
    fun build(): ConnectionPoolConfiguration = ConnectionPoolConfiguration(
        host = host,
        port = port,
        database = database,
        username = username,
        password = password,
        maxActiveConnections = maxActiveConnections,
        maxIdleTime = maxIdleTime,
        maxConnectionTtl = maxConnectionTtl,
        maxPendingQueries = maxPendingQueries,
        connectionValidationInterval = connectionValidationInterval,
        connectionCreateTimeout = connectionCreateTimeout,
        connectionTestTimeout = connectionTestTimeout,
        queryTimeout = queryTimeout,
        executionContext = executionContext,
        coroutineDispatcher = coroutineDispatcher,
        ssl = ssl,
        charset = charset,
        maximumMessageSize = maximumMessageSize,
        allocator = allocator,
        applicationName = applicationName,
        interceptors = interceptors
    )
}
