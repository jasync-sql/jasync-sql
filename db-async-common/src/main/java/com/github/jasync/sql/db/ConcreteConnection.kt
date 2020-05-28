package com.github.jasync.sql.db

import com.github.jasync.sql.db.interceptor.PreparedStatementParams
import com.github.jasync.sql.db.pool.PooledObject
import com.github.jasync.sql.db.pool.TimeoutScheduler
import java.util.concurrent.CompletableFuture

/**
 * An interface represents a connection driver (not a wrapper)
 */
interface ConcreteConnection : Connection, PooledObject, TimeoutScheduler {

    fun isQuerying(): Boolean

    fun lastException(): Throwable?

    fun hasRecentError(): Boolean

    fun sendQueryDirect(query: String): CompletableFuture<QueryResult>

    fun sendPreparedStatementDirect(params: PreparedStatementParams): CompletableFuture<QueryResult>
}
