package com.github.jasync.sql.db.pool

import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.Listener
import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.util.*
import java.lang.Exception
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.function.Supplier

/**
 *
 * Pool specialized in database connections that also simplifies connection handling by
 * implementing the <<com.github.mauricio.sql.db.Connection>> interface and saving clients from having to implement
 * the "give back" part of pool management. This lets you do your job ,out having to worry
 * about managing and giving back connection objects to the pool.
 *
 * The downside of this is that you should not start transactions or any kind of long running process
 * in this object as the object will be sent back to the pool right after executing a query. If you
 * need to start transactions you will have to take an object from the pool, do it and then give it
 * back manually.
 *
 * @param factory
 * @param configuration
 */

class ConnectionPool<T : Connection> @JvmOverloads constructor(
    factory: ObjectFactory<T>,
    val configuration: PoolConfiguration,
    private val listener: List<Supplier<Listener>> = emptyList(),
    private val executionContext: Executor = ExecutorServiceUtils.CommonPool
) : AsyncObjectPool<T>, Connection {

    private val objectPool = ActorBasedObjectPool(factory, configuration)

    override val id: String
        get() = XXX("not implemented as it is not a real connection")

    /**
     *
     * Closes the pool, you should discard the object.
     *
     * @return
     */

    override fun disconnect(): CompletableFuture<Connection> =
        if (this.isConnected()) {
            objectPool.close().mapAsync(executionContext) { this }
        } else {
            CompletableFuture.completedFuture(this)
        }

    /**
     *
     * Always returns an empty map.
     *
     * @return
     */

    override fun connect(): CompletableFuture<Connection> = CompletableFuture.completedFuture(this)

    override fun isConnected(): Boolean = !objectPool.closed

    /**
     *
     * Picks one connection and runs this query against it. The query should be stateless, it should not
     * start transactions and should not leave anything to be cleaned up in the future. The behavior of this
     * object is unfunined if you start a transaction from this method.
     *
     * @param query
     * @return
     */

    override fun sendQuery(query: String): CompletableFuture<QueryResult> =
        objectPool.use(executionContext) {
            for (listener in listener) {
                try {
                    listener.get().onQuery(query)
                } catch (ignore: Exception) {

                }
            }
            it.sendQuery(query).onComplete { r ->
                if (r.isSuccess) {
                    for (listener in listener) {
                        try {
                            listener.get().onQueryComplete(r.get())
                        } catch (ignore: Exception) {

                        }
                    }
                } else {
                    for (listener in listener) {
                        try {
                            listener.get().onQueryError(query)
                        } catch (ignore: Exception) {

                        }
                    }
                }
                r.asCompletedFuture()
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
    override fun sendPreparedStatement(query: String, values: List<Any?>, release: Boolean): CompletableFuture<QueryResult> =
        objectPool.use(executionContext) { it.sendPreparedStatement(query, values) }

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

    @Deprecated("use idleConnectionsCount")
    fun availables(): List<T> = objectPool.availableItems

    @Deprecated("use futuresWaitingForConnectionCount")
    fun queued(): List<CompletableFuture<T>> = objectPool.waitingForItem

    @Deprecated("use inUseConnectionsCount")
    fun inUse(): List<T> = objectPool.usedItems

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

    override fun take(): CompletableFuture<T> = objectPool.take()

    override fun giveBack(item: T): CompletableFuture<AsyncObjectPool<T>> = objectPool.giveBack(item)

    override fun close(): CompletableFuture<AsyncObjectPool<T>> = objectPool.close()
}
