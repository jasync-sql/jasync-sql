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
import java.nio.file.Path
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
 * @param rsaPublicKey path to the RSA public key, used for password encryption over unsafe connections
 *
 */
class Configuration @JvmOverloads constructor(
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
    val credentialsProvider: CredentialsProvider? = null,
    val rsaPublicKey: Path? = null,
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
    fun copy(
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
        rsaPublicKey: Path? = null,
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
            rsaPublicKey = rsaPublicKey ?: this.rsaPublicKey,
        )
    }

    @Deprecated(
        "backward compatibility for https://github.com/jasync-sql/jasync-sql/issues/359",
        ReplaceWith("copy()"),
        DeprecationLevel.ERROR,
    )
    fun copy(
        username: String? = null,
        host: String? = null,
        port: Int? = null,
        password: String? = null,
        database: String? = null,
        ssl: SSLConfiguration? = null,
        charset: Charset? = null,
        maximumMessageSize: Int,
        allocator: ByteBufAllocator? = null,
        connectionTimeout: Int,
        queryTimeout: Duration? = null,
        applicationName: String? = null,
        interceptors: List<Supplier<QueryInterceptor>>? = null,
        eventLoopGroup: EventLoopGroup? = null,
        executionContext: Executor? = null,
        currentSchema: String? = null,
        socketPath: String? = null,
    ): Configuration {
        return Configuration(
            username = username ?: this.username,
            host = host ?: this.host,
            port = port ?: this.port,
            password = password ?: this.password,
            database = database ?: this.database,
            ssl = ssl ?: this.ssl,
            charset = charset ?: this.charset,
            maximumMessageSize = maximumMessageSize,
            allocator = allocator ?: this.allocator,
            connectionTimeout = connectionTimeout,
            queryTimeout = queryTimeout ?: this.queryTimeout,
            applicationName = applicationName ?: this.applicationName,
            interceptors = interceptors ?: this.interceptors,
            eventLoopGroup = eventLoopGroup ?: this.eventLoopGroup,
            executionContext = executionContext ?: this.executionContext,
            currentSchema = currentSchema ?: this.currentSchema,
            socketPath = socketPath ?: this.socketPath,
            credentialsProvider = this.credentialsProvider,
            rsaPublicKey = this.rsaPublicKey,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Configuration

        if (username != other.username) return false
        if (host != other.host) return false
        if (port != other.port) return false
        if (password != other.password) return false
        if (database != other.database) return false
        if (ssl != other.ssl) return false
        if (charset != other.charset) return false
        if (maximumMessageSize != other.maximumMessageSize) return false
        if (allocator != other.allocator) return false
        if (connectionTimeout != other.connectionTimeout) return false
        if (queryTimeout != other.queryTimeout) return false
        if (applicationName != other.applicationName) return false
        if (interceptors != other.interceptors) return false
        if (eventLoopGroup != other.eventLoopGroup) return false
        if (executionContext != other.executionContext) return false
        if (currentSchema != other.currentSchema) return false
        if (socketPath != other.socketPath) return false
        if (credentialsProvider != other.credentialsProvider) return false
        if (rsaPublicKey != other.rsaPublicKey) return false

        return true
    }

    override fun hashCode(): Int {
        var result = username.hashCode()
        result = 31 * result + host.hashCode()
        result = 31 * result + port
        result = 31 * result + (password?.hashCode() ?: 0)
        result = 31 * result + (database?.hashCode() ?: 0)
        result = 31 * result + ssl.hashCode()
        result = 31 * result + charset.hashCode()
        result = 31 * result + maximumMessageSize
        result = 31 * result + allocator.hashCode()
        result = 31 * result + connectionTimeout
        result = 31 * result + (queryTimeout?.hashCode() ?: 0)
        result = 31 * result + (applicationName?.hashCode() ?: 0)
        result = 31 * result + interceptors.hashCode()
        result = 31 * result + eventLoopGroup.hashCode()
        result = 31 * result + executionContext.hashCode()
        result = 31 * result + (currentSchema?.hashCode() ?: 0)
        result = 31 * result + (socketPath?.hashCode() ?: 0)
        result = 31 * result + (credentialsProvider?.hashCode() ?: 0)
        result = 31 * result + (rsaPublicKey?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "Configuration(username='$username', host='$host', port=$port, password=****, database=$database, ssl=$ssl, charset=$charset, maximumMessageSize=$maximumMessageSize, allocator=$allocator, connectionTimeout=$connectionTimeout, queryTimeout=$queryTimeout, applicationName=$applicationName, interceptors=$interceptors, eventLoopGroup=$eventLoopGroup, executionContext=$executionContext, currentSchema=$currentSchema, socketPath=$socketPath, credentialsProvider=$credentialsProvider, rsaPublicKey=$rsaPublicKey)"
    }
}

@Deprecated(message = "not required anymore as password is not printed in toString", replaceWith = ReplaceWith("toString()"))
fun Configuration.toDebugString(): String {
    return this.copy(password = "****").toString()
}
