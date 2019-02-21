package com.github.jasync.sql.db

import com.github.jasync.sql.db.interceptor.PreparedStatementParams
import com.github.jasync.sql.db.interceptor.wrapPreparedStatementWithInterceptors
import com.github.jasync.sql.db.interceptor.wrapQueryWithInterceptors
import com.github.jasync.sql.db.util.FP
import com.github.jasync.sql.db.util.Failure
import com.github.jasync.sql.db.util.complete
import com.github.jasync.sql.db.util.failed
import com.github.jasync.sql.db.util.flatMapAsync
import com.github.jasync.sql.db.util.flatMapTry
import com.github.jasync.sql.db.util.map
import com.github.jasync.sql.db.util.onCompleteAsync
import java.util.concurrent.CompletableFuture

abstract class ConcreteConnectionBase(
    val configuration: Configuration
) : ConcreteConnection {

    override val creationTime: Long = System.currentTimeMillis()

    override fun <A> inTransaction(f: (Connection) -> CompletableFuture<A>): CompletableFuture<A> {
        return this.sendQuery("BEGIN").flatMapAsync(configuration.executionContext) {
            val p = CompletableFuture<A>()
            f(this).onCompleteAsync(configuration.executionContext) { ty1 ->
                sendQuery(if (ty1.isFailure) "ROLLBACK" else "COMMIT").onCompleteAsync(configuration.executionContext) { ty2 ->
                    if (ty2.isFailure && ty1.isSuccess)
                        p.failed((ty2 as Failure).exception)
                    else
                        p.complete(ty1)
                }
            }
            p
        }
    }

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

    override fun sendQuery(query: String): CompletableFuture<QueryResult> {
        return wrapQueryWithInterceptors(query, configuration.interceptors) { q ->
            sendQueryDirect(q)
        }
    }

    override fun sendPreparedStatement(
        query: String,
        values: List<Any?>,
        release: Boolean
    ): CompletableFuture<QueryResult> {
        return wrapPreparedStatementWithInterceptors(
            PreparedStatementParams(
                query,
                values,
                release
            ),
            configuration.interceptors
        ) { params ->
            sendPreparedStatementDirect(params)
        }
    }

    abstract override fun sendQueryDirect(query: String): CompletableFuture<QueryResult>

    abstract override fun sendPreparedStatementDirect(params: PreparedStatementParams): CompletableFuture<QueryResult>


}
