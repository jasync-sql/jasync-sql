package com.github.jasync.sql.db

import com.github.jasync.sql.db.util.Failure
import com.github.jasync.sql.db.util.complete
import com.github.jasync.sql.db.util.failed
import com.github.jasync.sql.db.util.flatMapAsync
import com.github.jasync.sql.db.util.onCompleteAsync
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.function.Supplier

internal fun <A> Connection.inTransaction(executor: Executor, f: (Connection) -> CompletableFuture<A>): CompletableFuture<A> {
    return this.sendQuery("BEGIN").flatMapAsync(executor) {
        val p = CompletableFuture<A>()
        f(this).onCompleteAsync(executor) { ty1 ->
            sendQuery(if (ty1.isFailure) "ROLLBACK" else "COMMIT").onCompleteAsync(executor) { ty2 ->
                if (ty2.isFailure && ty1.isSuccess)
                    p.failed((ty2 as Failure).exception)
                else
                    p.complete(ty1)
            }
        }
        p
    }
}

internal fun wrapQueryWithInterceptors(
    query: String,
    interceptors: List<Supplier<QueryInterceptor>>,
    fn: (String) -> CompletableFuture<QueryResult>
): CompletableFuture<QueryResult> {
    if (interceptors.isEmpty()) {
        return fn(query)
    }
    val resolvedListeners = interceptors.map { it.get() }
    var currentQuery = query
    for (listener in resolvedListeners) {
        currentQuery = listener.interceptQuery(query)
    }
    var currentResult = fn(currentQuery)
    for (listener in resolvedListeners.reversed()) {
        currentResult = listener.interceptQueryComplete(currentResult)
    }
    return currentResult
}

internal fun wrapPreparedStatementWithInterceptors(
    preparedStatementParams: PreparedStatementParams,
    interceptors: List<Supplier<QueryInterceptor>>,
    fn: (PreparedStatementParams) -> CompletableFuture<QueryResult>
): CompletableFuture<QueryResult> {
    if (interceptors.isEmpty()) {
        return fn(preparedStatementParams)
    }
    val resolvedListeners = interceptors.map { it.get() }
    var currentParams = preparedStatementParams
    for (listener in resolvedListeners) {
        currentParams = listener.interceptPreparedStatement(currentParams)
    }
    var currentResult = fn(currentParams)
    for (listener in resolvedListeners.reversed()) {
        currentResult = listener.interceptPreparedStatementComplete(currentResult)
    }
    return currentResult
}

