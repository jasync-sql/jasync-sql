package com.github.jasync.sql.db.pool

import com.github.jasync.sql.db.ConcreteConnection
import com.github.jasync.sql.db.ConcreteConnectionBase
import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.ConnectionPoolConfiguration
import com.github.jasync.sql.db.PreparedStatementParams
import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.util.ExecutorServiceUtils
import com.github.jasync.sql.db.util.FP
import com.github.jasync.sql.db.util.mapAsync
import com.github.jasync.sql.db.wrapPreparedStatementWithInterceptors
import com.github.jasync.sql.db.wrapQueryWithInterceptors
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

/**
 *
 * Pool specialized in database connections that also simplifies connection handling by
 * implementing the <<Connection>> interface and saving clients from having to implement
 * the "give back" part of pool management. This lets you do your job without having to worry
 * about managing and giving back connection objects to the pool.
 *
 * @param factory
 * @param configuration
 */

class ConnectionPool<T : ConcreteConnection> @JvmOverloads constructor(
    factory: ObjectFactory<T>,
    val connectionPoolConfiguration: ConnectionPoolConfiguration,
    private val executionContext: Executor = ExecutorServiceUtils.CommonPool
) : Connection {

    private val objectPool = ActorBasedObjectPool(factory, connectionPoolConfiguration.poolConfiguration)

    /**
     *
     * Picks one connection and runs this query against it. The query should be stateless, it should not
     * start transactions and should not leave anything to be cleaned up in the future. The behavior of this
     * object is undefined if you start a transaction from this method.
     *
     * @param query
     * @return
     */

    override fun sendQuery(query: String): CompletableFuture<QueryResult> =
        wrapQueryWithInterceptors(query, connectionPoolConfiguration.interceptors) { q ->
            objectPool.use(executionContext) { connection ->
                (connection as ConcreteConnectionBase).sendQueryInternal(q)
            }
        }

    /**
     *
     * Picks one connection and runs this query against it. The query should be stateless, it should not
     * start transactions and should not leave anything to be cleaned up in the future. The behavior of this
     * object is unfunined if you start a transaction from this method.
     *
     * @param query
     * @param values
     * @return
     */
    override fun sendPreparedStatement(
        query: String,
        values: List<Any?>,
        release: Boolean
    ): CompletableFuture<QueryResult> =
        wrapPreparedStatementWithInterceptors(
            PreparedStatementParams(query, values, release),
            connectionPoolConfiguration.interceptors
        ) { params ->
            objectPool.use(executionContext) { connection ->
                (connection as ConcreteConnectionBase).sendPreparedStatementInternal(params)
            }
        }

    /**
     * This method always return false as it doesn't know from what connection to release the query
     */
    override fun releasePreparedStatement(query: String): CompletableFuture<Boolean> = FP.successful(false)

    /**
     *
     * Picks one connection and executes an (asynchronous) function on it ,in a transaction block.
     * If the function completes successfully, the transaction is committed, otherwise it is aborted.
     * Either way, the connection is returned to the pool on completion.
     *
     * @param f operation to execute on a connection
     * @return result of f, conditional on transaction operations succeeding
     */

    override fun <A> inTransaction(f: (Connection) -> CompletableFuture<A>)
            : CompletableFuture<A> =
        objectPool.use(executionContext) { it.inTransaction(f) }

    /**
     * The number of connections that are currently in use for queries
     */
    val inUseConnectionsCount: Int get() = objectPool.usedItemsSize

    /**
     * The number of futures that were submitted by not yet assigned to a connection from the pool
     */
    val futuresWaitingForConnectionCount: Int get() = objectPool.waitingForItemSize

    /**
     * The number of connections that are idle and ready to use
     */
    val idleConnectionsCount: Int get() = objectPool.availableItemsSize

    fun take(): CompletableFuture<T> = objectPool.take()

    fun giveBack(item: T): CompletableFuture<AsyncObjectPool<T>> = objectPool.giveBack(item)

    /**
     * Closes the pool.
     */
    override fun disconnect(): CompletableFuture<Connection> =
        if (this.isConnected()) {
            objectPool.close().mapAsync(executionContext) { this }
        } else {
            CompletableFuture.completedFuture(this)
        }


    override fun connect(): CompletableFuture<Connection> = CompletableFuture.completedFuture(this)

    override fun isConnected(): Boolean = !objectPool.closed

}
