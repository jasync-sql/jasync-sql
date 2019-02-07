package com.github.jasync.sql.db

import com.github.jasync.sql.db.util.mapTry
import org.slf4j.MDC
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

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

data class PreparedStatementParams(
    val query: String,
    val values: List<Any?>,
    val release: Boolean
)

class MdcQueryInterceptorSupplier : Supplier<QueryInterceptor> {
    override fun get(): QueryInterceptor {
        return object: QueryInterceptor {
            private val context: Map<String, String>? = MDC.getCopyOfContextMap()

            override fun interceptQuery(query: String): String {
                return query
            }

            override fun interceptQueryComplete(result: CompletableFuture<QueryResult>): CompletableFuture<QueryResult> {
                return result.withMdcContext()
            }

            override fun interceptPreparedStatement(params: PreparedStatementParams): PreparedStatementParams {
                return params
            }

            override fun interceptPreparedStatementComplete(result: CompletableFuture<QueryResult>): CompletableFuture<QueryResult> {
                return result.withMdcContext()
            }

            private fun CompletableFuture<QueryResult>.withMdcContext(): CompletableFuture<QueryResult> {
                return mapTry { queryResult, throwable ->
                    context?.apply { MDC.setContextMap(this) }
                    if (throwable == null) {
                        queryResult
                    } else {
                        throw throwable
                    }
                }
            }

        }
    }


}