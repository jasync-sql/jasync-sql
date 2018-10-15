package com.github.jasync.sql.db.pool;

import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.util.ExecutorServiceUtils
import com.github.jasync.sql.db.util.XXX
import com.github.jasync.sql.db.util.map
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor


class PartitionedConnectionPool<T : Connection>(
    factory: ObjectFactory<T>,
    configuration: PoolConfiguration,
    numberOfPartitions: Int,
    private val executionContext: Executor = ExecutorServiceUtils.CommonPool)
  : PartitionedAsyncObjectPool<T>(factory, configuration, numberOfPartitions, executionContext)
    , Connection {

  override val id: String
    get() = XXX("not implemented as it is not a real connection")

  override fun disconnect(): CompletableFuture<Connection> =
      if (this.isConnected()) {
        this.close().map(executionContext) { this }
      } else {
        CompletableFuture.completedFuture(this)
      }

  override fun connect(): CompletableFuture<Connection> = CompletableFuture.completedFuture(this)

  override fun isConnected(): Boolean = !this.isClosed()

  override fun sendQuery(query: String): CompletableFuture<QueryResult> =
      this.use(executionContext) { it.sendQuery(query) }

  override fun sendPreparedStatement(query: String, values: List<Any?>): CompletableFuture<QueryResult> =
      this.use(executionContext) { it.sendPreparedStatement(query, values) }

  override fun <A> inTransaction(f: (Connection) -> CompletableFuture<A>): CompletableFuture<A> =
      this.use(executionContext) { it.inTransaction(f) }
}
