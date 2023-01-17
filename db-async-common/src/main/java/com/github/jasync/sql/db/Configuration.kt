package com.github.jasync.sql.db

import com.github.jasync.sql.db.interceptor.QueryInterceptor
import com.github.jasync.sql.db.util.ExecutorServiceUtils
import com.github.jasync.sql.db.util.NettyUtils
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.util.CharsetUtil
import mu.KotlinLogging
import java.nio.charset.Charset
import java.time.Duration
import java.util.concurrent.CompletionStage
import java.util.concurrent.Executor
import java.util.function.Supplier

private val logger = KotlinLogging.logger {}

/**
 *
 * Contains the configuration necessary to connect to a database.
 *
 * @param username database username
 * @param host database host, defaults to "localhost"
 * @param port database port, defaults to 5432
 * @param password password, defaults to no password
 * @param database database name, defaults to no database
 * @param ssl ssl configuration
 * @param charset charset for the connection, defaults to UTF-8, make sure you know what you are doing if you
 *                change this
 * @param maximumMessageSize the maximum size a message from the server could possibly have, this limits possible
 *                           OOM or eternal loop attacks the client could have, defaults to 16 MB. You can set this
 *                           to any value you would like but again, make sure you know what you are doing if you do
 *                           change it.
 * @param allocator the netty buffer allocator to be used
 * @param connectionTimeout the timeout for connecting to servers in millis
 * @param queryTimeout the optional query timeout. If it's null there will be no query timeout at all
 * @param applicationName optional name to be passed to the database for reporting
 * @param interceptors optional delegates to call on query execution
 * @param executionContext the thread pool to run the callbacks on
 * @param eventLoopGroup the netty event loop group - use this to select native/nio transport.
 * @param currentSchema optional database schema - postgresql only.
 * @param socketPath path to unix domain socket file (on the local machine)
 * @param credentialsProvider a credential provider used to inject credentials on demand
 *
 */
data class Configuration @JvmOverloads constructor(
    val username: String,
    val host: String = "localhost",
    val port: Int = 5432,
    val password: String? = null,
    val database: String? = null,
    val ssl: SSLConfiguration = SSLConfiguration(),
    val charset: Charset = CharsetUtil.UTF_8,
    val maximumMessageSize: Int = 16777216,
    val allocator: ByteBufAllocator = PooledByteBufAllocator.DEFAULT,
    val connectionTimeout: Int = 5000,
    val queryTimeout: Duration? = null,
    val applicationName: String? = null,
    val interceptors: List<Supplier<QueryInterceptor>> = emptyList(),
    val eventLoopGroup: EventLoopGroup = NettyUtils.DefaultEventLoopGroup,
    val executionContext: Executor = ExecutorServiceUtils.CommonPool,
    val currentSchema: String? = null,
    val socketPath: String? = null,
    val credentialsProvider: CredentialsProvider? = null
) {
    init {
        if (socketPath != null && eventLoopGroup is NioEventLoopGroup) {
            logger.warn {
                "socketPath configured but not supported with NioEventLoopGroup - will ignore configuration. " +
                    "Please change eventLoopGroup configuration."
            }
        }
    }

    fun resolveCredentials(): CompletionStage<Credentials> = (credentialsProvider ?: StaticCredentialsProvider(username, password)).provide()

    @JvmOverloads
    fun copyConfiguration(
        username: String? = null,
        host: String? = null,
        port: Int? = null,
        password: String? = null,
        database: String? = null,
        ssl: SSLConfiguration? = null,
        charset: Charset? = null,
        maximumMessageSize: Int? = null,
        allocator: ByteBufAllocator? = null,
        connectionTimeout: Int? = null,
        queryTimeout: Duration? = null,
        applicationName: String? = null,
        interceptors: List<Supplier<QueryInterceptor>>? = null,
        eventLoopGroup: EventLoopGroup? = null,
        executionContext: Executor? = null,
        currentSchema: String? = null,
        socketPath: String? = null,
        credentialsProvider: CredentialsProvider? = null,
    ): Configuration {
        return Configuration(
            username = username ?: this.username,
            host = host ?: this.host,
            port = port ?: this.port,
            password = password ?: this.password,
            database = database ?: this.database,
            ssl = ssl ?: this.ssl,
            charset = charset ?: this.charset,
            maximumMessageSize = maximumMessageSize ?: this.maximumMessageSize,
            allocator = allocator ?: this.allocator,
            connectionTimeout = connectionTimeout ?: this.connectionTimeout,
            queryTimeout = queryTimeout ?: this.queryTimeout,
            applicationName = applicationName ?: this.applicationName,
            interceptors = interceptors ?: this.interceptors,
            eventLoopGroup = eventLoopGroup ?: this.eventLoopGroup,
            executionContext = executionContext ?: this.executionContext,
            currentSchema = currentSchema ?: this.currentSchema,
            socketPath = socketPath ?: this.socketPath,
            credentialsProvider = credentialsProvider ?: this.credentialsProvider,
        )
    }
}

fun Configuration.toDebugString(): String {
    return this.copy(password = "****").toString()
}
