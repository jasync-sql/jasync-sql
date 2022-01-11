package com.github.jasync.sql.db.interceptor

import com.github.jasync.sql.db.QueryResult
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

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
