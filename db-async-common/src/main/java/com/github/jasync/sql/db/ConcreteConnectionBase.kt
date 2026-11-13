package com.github.jasync.sql.db

import com.github.jasync.sql.db.interceptor.PreparedStatementParams
import com.github.jasync.sql.db.interceptor.wrapPreparedStatementWithInterceptors
import com.github.jasync.sql.db.interceptor.wrapQueryWithInterceptors
import com.github.jasync.sql.db.util.FP
import com.github.jasync.sql.db.util.Failure
import com.github.jasync.sql.db.util.Try
import com.github.jasync.sql.db.util.complete
import com.github.jasync.sql.db.util.failed
import com.github.jasync.sql.db.util.flatMapAsync
import com.github.jasync.sql.db.util.flatMapTry
import com.github.jasync.sql.db.util.map
import com.github.jasync.sql.db.util.onCompleteAsync
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

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
                    rollbackAfterFailure((ty1 as Failure).exception, p)
                } else {
                    commitAfterSuccess(ty1, p)
                }
            }
            p
        }
    }

    private fun <A> rollbackAfterFailure(rawException: Throwable, p: CompletableFuture<A>) {
        val origException = unwrapCompletionException(rawException)
        if (!this.isConnected() || this.isTimeout() || this.isQuerying()) {
            if (this.isConnected() || this.isQuerying()) {
                runCatching { this.disconnect() }.onFailure { disconnectEx ->
                    addSuppressedIfDifferent(origException, disconnectEx)
                }
            }
            p.failed(origException)
            return
        }
        try {
            sendQuery("ROLLBACK").onCompleteAsync(configuration.executionContext) { ty2 ->
                if (ty2.isFailure) {
                    addSuppressedIfDifferent(origException, (ty2 as Failure).exception)
                }
                p.failed(origException)
            }
        } catch (rollbackEx: Throwable) {
            addSuppressedIfDifferent(origException, rollbackEx)
            p.failed(origException)
        }
    }

    private fun <A> commitAfterSuccess(result: Try<A>, p: CompletableFuture<A>) {
        try {
            sendQuery("COMMIT").onCompleteAsync(configuration.executionContext) { ty2 ->
                if (ty2.isFailure) {
                    p.failed((ty2 as Failure).exception)
                } else {
                    p.complete(result)
                }
            }
        } catch (commitEx: Throwable) {
            p.failed(commitEx)
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

internal fun unwrapCompletionException(throwable: Throwable): Throwable =
    (throwable as? CompletionException)?.cause ?: throwable

internal fun addSuppressedIfDifferent(primary: Throwable, secondary: Throwable) {
    val unwrappedSecondary = unwrapCompletionException(secondary)
    if (primary !== unwrappedSecondary) {
        primary.addSuppressed(unwrappedSecondary)
    }
}
