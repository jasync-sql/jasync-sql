package com.github.jasync.sql.db.pool

import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.util.ExecutorServiceUtils
import com.github.jasync.sql.db.util.map
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

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
    configuration: PoolConfiguration,
    private val executionContext: Executor = ExecutorServiceUtils.CommonPool
) : SingleThreadedAsyncObjectPool<T>(factory, configuration)
    , Connection {

  /**
   *
   * Closes the pool, you should discard the object.
   *
   * @return
   */

  override fun disconnect(): CompletableFuture<Connection> =
      if (this.isConnected()) {
        this.close().map(executionContext) { item -> this }
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

  override fun isConnected(): Boolean = !this.isClosed()

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
      this.use(executionContext) { it.sendQuery(query) }

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
//values: List<Any> = emptyList()
  override fun sendPreparedStatement(query: String, values: List<Any?>): CompletableFuture<QueryResult> =
      this.use(executionContext) { it.sendPreparedStatement(query, values) }

  /**
   *
   * Picks one connection and executes an (asynchronous) function on it ,in a transaction block.
   * If the function completes successfully, the transaction is committed, otherwise it is aborted.
   * Either way, the connection is returned to the pool on completion.
   *
   * @param f operation to execute on a connection
   * @return result of f, conditional on transaction operations succeeding
   */

  override fun <A> inTransaction(executor: Executor, f: (Connection) -> CompletableFuture<A>)
  //(implicit context : ExecutionContext = executionContext)
      : CompletableFuture<A> =
      this.use(executionContext) { it.inTransaction(executor, f) }

}
