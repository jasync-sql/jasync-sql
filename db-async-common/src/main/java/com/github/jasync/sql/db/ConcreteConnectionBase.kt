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
            val actionFuture = try {
                f(this)
            } catch (e: Throwable) {
                FP.failed(e)
            }
            actionFuture.onCompleteAsync(configuration.executionContext) { ty1 ->
                if (ty1.isFailure) {
                    val origException = (ty1 as Failure).exception
                    if (!this.isConnected() || this.isTimeout()) {
                        if (this.isConnected()) {
                            runCatching { this.disconnect() }
                        }
                        p.complete(ty1)
                    } else if (this.isQuerying()) {
                        runCatching { this.disconnect() }.onFailure { disconnectEx ->
                            if (origException !== disconnectEx) {
                                origException.addSuppressed(disconnectEx)
                            }
                        }
                        p.complete(ty1)
                    } else {
                        try {
                            sendQuery("ROLLBACK").onCompleteAsync(configuration.executionContext) { ty2 ->
                                if (ty2.isFailure) {
                                    val rollbackEx = (ty2 as Failure).exception
                                    if (origException !== rollbackEx) {
                                        origException.addSuppressed(rollbackEx)
                                    }
                                }
                                p.complete(ty1)
                            }
                        } catch (rollbackEx: Throwable) {
                            if (origException !== rollbackEx) {
                                origException.addSuppressed(rollbackEx)
                            }
                            p.complete(ty1)
                        }
                    }
                } else {
                    try {
                        sendQuery("COMMIT").onCompleteAsync(configuration.executionContext) { ty2 ->
                            if (ty2.isFailure) {
                                p.failed((ty2 as Failure).exception)
                            } else {
                                p.complete(ty1)
                            }
                        }
                    } catch (commitEx: Throwable) {
                        p.failed(commitEx)
                    }
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
                if (throwable != null) {
                    if (this.isConnected() && !this.isTimeout() && !this.isQuerying()) {
                        runCatching { this.releasePreparedStatement(query) }
                    }
                    FP.failed(throwable)
                } else {
                    this.releasePreparedStatement(query).map { queryResult }
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
