package com.github.jasync.sql.db.interceptor

import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.util.mapTry
import mu.KotlinLogging
import org.slf4j.MDC
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

/**
 * An interceptor that passes MDC context around the queries
 */
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

/**
 * An interceptor that print sql to logs
 */
class LoggingInterceptorSupplier : Supplier<QueryInterceptor> {
    private val logger = KotlinLogging.logger ("com.github.jasync.sql.QueryLog")

    override fun get(): QueryInterceptor {
        return object: QueryInterceptor {

            override fun interceptQuery(query: String): String {
                logger.debug { "sendQuery: $query" }
                return query
            }

            override fun interceptQueryComplete(result: CompletableFuture<QueryResult>): CompletableFuture<QueryResult> {
                return result
            }

            override fun interceptPreparedStatement(params: PreparedStatementParams): PreparedStatementParams {
                logger.debug { "preparedStatement: $params" }
                return params
            }

            override fun interceptPreparedStatementComplete(result: CompletableFuture<QueryResult>): CompletableFuture<QueryResult> {
                return result
            }

        }
    }


}
