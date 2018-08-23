
package com.github.jasync.sql.db

import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.PooledByteBufAllocator
import io.netty.util.CharsetUtil
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit


/**
 *
 * Contains the configuration necessary to connect to a database.
 *
 * @param username database username
 * @param host database host, funaults to "localhost"
 * @param port database port, funaults to 5432
 * @param password password, funaults to no password
 * @param database database name, funaults to no database
 * @param ssl ssl configuration
 * @param charset charset for the connection, funaults to UTF-8, make sure you know what you are doing if you
 *                change this
 * @param maximumMessageSize the maximum size a message from the server could possibly have, this limits possible
 *                           OOM or eternal loop attacks the client could have, funaults to 16 MB. You can set this
 *                           to any value you would like but again, make sure you know what you are doing if you do
 *                           change it.
 * @param allocator the netty buffer allocator to be used
 * @param connectTimeout the timeout for connecting to servers
 * @param testTimeout the timeout for connection tests performed by pools
 * @param queryTimeout the optional query timeout
 *
 */

data class Configuration(val username: String,
                         val host: String = "localhost",
                         val port: Int = 5432,
                         val password: String? = null,
                         val database: String? = null,
                         val ssl: SSLConfiguration = SSLConfiguration(),
                         val charset: Charset = Configuration.DefaultCharset,
                         val maximumMessageSize: Int = 16777216,
                         val allocator: ByteBufAllocator = PooledByteBufAllocator.DEFAULT,
                         val connectTimeout: Long = TimeUnit.SECONDS.toMillis(5),// = scala.concurrent.duration.Duration.apply(5, TimeUnit.SECONDS),
                         val testTimeout: Long= TimeUnit.SECONDS.toMillis(5), //5, TimeUnit.SECONDS),
                         val queryTimeout: Long? = null) {
  companion object {
    val DefaultCharset = CharsetUtil.UTF_8

    @Deprecated("Use com.github.jasync.sql.db.postgresql.util.URLParser.DEFAULT or com.github.jasync.sql.db.mysql.util.URLParser.DEFAULT.")
    val Default = Configuration("postgres")
  }
}
