package com.github.jasync.sql.db.interceptor

import com.github.jasync.sql.db.QueryResult
import java.util.concurrent.CompletableFuture

interface QueryInterceptor {

    /**
     * called before query executed
     * @param query that should be executed
     * @return the query that should be executed or a manipulation over it
     */
    fun interceptQuery(query: String): String

    /**
     * called with query result, allow to hook into result, change them or introspect them
     * @param result a future holding the result
     * @return the future holding the result or a manipulation over it
     */
    fun interceptQueryComplete(result: CompletableFuture<QueryResult>): CompletableFuture<QueryResult>

    /**
     * called before prepared statement executed
     * @param params that should be executed
     * @return the prepared statement that should be executed or a manipulation over it
     */
    fun interceptPreparedStatement(params: PreparedStatementParams): PreparedStatementParams

    /**
     * called with prepared statement result, allow to hook into result, change them or introspect them
     * @param result a future holding the result
     * @return the future holding the result or a manipulation over it
     */
    fun interceptPreparedStatementComplete(result: CompletableFuture<QueryResult>): CompletableFuture<QueryResult>
}
