package com.github.jasync.sql.db.mysql.pool

import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.mysql.MySQLConnection
import com.github.jasync.sql.db.pool.ConnectionFactory
import com.github.jasync.sql.db.util.ExecutorServiceUtils
import com.github.jasync.sql.db.util.NettyUtils
import io.netty.channel.EventLoopGroup
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
) : ConnectionFactory<MySQLConnection>() {

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

}

