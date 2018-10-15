package com.github.jasync.sql.db

import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.PooledByteBufAllocator
import io.netty.util.CharsetUtil
import java.nio.charset.Charset
import java.time.Duration


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
 * @param connectTimeout the timeout for connecting to servers
 * @param testTimeout the timeout for connection tests performed by pools
 * @param queryTimeout the optional query timeout
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
    @Deprecated("use PoolConfiguration.createTimeout")
    val connectTimeout: Duration = Duration.ofSeconds(5),
    @Deprecated("use PoolConfiguration.testTimeout")
    val testTimeout: Duration = Duration.ofSeconds(5),
    val queryTimeout: Duration? = null
)
