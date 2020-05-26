package com.github.aysnc.sql.db.integration

import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.interceptor.PreparedStatementParams
import com.github.jasync.sql.db.interceptor.QueryInterceptor
import com.github.jasync.sql.db.util.mapTry
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

class ForTestingQueryInterceptor : QueryInterceptor {
    val queries = AtomicInteger()
    val completedQueries = AtomicInteger()

    val preparedStatements = AtomicInteger()
    val completedPreparedStatements = AtomicInteger()

    override fun interceptQuery(query: String): String {
        queries.getAndIncrement()
        return query
    }

    override fun interceptQueryComplete(result: CompletableFuture<QueryResult>): CompletableFuture<QueryResult> {
        return result.mapTry { r, t ->
            completedQueries.getAndIncrement()
            if (t != null) {
                throw t
            } else {
                r
            }
        }
    }

    override fun interceptPreparedStatement(params: PreparedStatementParams): PreparedStatementParams {
        preparedStatements.incrementAndGet()
        return params
    }

    override fun interceptPreparedStatementComplete(result: CompletableFuture<QueryResult>): CompletableFuture<QueryResult> {
        return result.mapTry { r, t ->
            completedPreparedStatements.getAndIncrement()
            if (t != null) {
                throw t
            } else {
                r
            }
        }
    }
}
