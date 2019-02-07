package com.github.jasync.sql.db

import com.github.jasync.sql.db.util.FP
import com.github.jasync.sql.db.util.flatMapTry
import com.github.jasync.sql.db.util.map
import mu.KotlinLogging
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

private val logger = KotlinLogging.logger {}

abstract class ConcreteConnectionBase(
    val configuration: Configuration,
    val executionContext: Executor
) : ConcreteConnection {

    protected fun releaseIfNeeded(
        release: Boolean,
        promise: CompletableFuture<QueryResult>,
        query: String
    ): CompletableFuture<QueryResult> {
        return if (!release) {
            promise
        } else {
            promise.flatMapTry { queryResult, throwable ->
                val released = this.releasePreparedStatement(query).map { queryResult }
                if (throwable != null) {
                    FP.failed(throwable)
                } else {
                    released
                }
            }
        }
    }


    override fun <A> inTransaction(f: (Connection) -> CompletableFuture<A>): CompletableFuture<A> =
        this.inTransaction(executionContext, f)

    override fun sendQuery(query: String): CompletableFuture<QueryResult> {
        return this.wrapQueryWithInterceptorsInternal(query) { q ->
            sendQueryInternal(q)
        }
    }

    override fun sendPreparedStatement(
        query: String,
        values: List<Any?>,
        release: Boolean
    ): CompletableFuture<QueryResult> {
        return this.wrapPreparedStatementWithInterceptorsInternal(
            PreparedStatementParams(
                query,
                values,
                release
            )
        ) { params ->
            sendPreparedStatementInternal(params)
        }
    }

    abstract fun sendQueryInternal(query: String): CompletableFuture<QueryResult>

    abstract fun sendPreparedStatementInternal(params: PreparedStatementParams): CompletableFuture<QueryResult>

    private fun wrapQueryWithInterceptorsInternal(
        query: String,
        fn: (String) -> CompletableFuture<QueryResult>
    ): CompletableFuture<QueryResult> {
        return wrapQueryWithInterceptors(query, configuration.interceptors, fn)
    }

    private fun wrapPreparedStatementWithInterceptorsInternal(
        preparedStatementParams: PreparedStatementParams,
        fn: (PreparedStatementParams) -> CompletableFuture<QueryResult>
    ): CompletableFuture<QueryResult> {
        return wrapPreparedStatementWithInterceptors(preparedStatementParams, configuration.interceptors, fn)
    }
}