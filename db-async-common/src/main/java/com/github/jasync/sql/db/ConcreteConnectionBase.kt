package com.github.jasync.sql.db

import com.github.jasync.sql.db.util.FP
import com.github.jasync.sql.db.util.Failure
import com.github.jasync.sql.db.util.complete
import com.github.jasync.sql.db.util.failed
import com.github.jasync.sql.db.util.flatMapAsync
import com.github.jasync.sql.db.util.flatMapTry
import com.github.jasync.sql.db.util.map
import com.github.jasync.sql.db.util.onCompleteAsync
import mu.KotlinLogging
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

private val logger = KotlinLogging.logger {}

abstract class ConcreteConnectionBase(
    val configuration: Configuration,
    val executionContext: Executor
) : ConcreteConnection {

    override fun <A> inTransaction(f: (Connection) -> CompletableFuture<A>): CompletableFuture<A> {
        return this.sendQuery("BEGIN").flatMapAsync(executionContext) {
            val p = CompletableFuture<A>()
            f(this).onCompleteAsync(executionContext) { ty1 ->
                sendQuery(if (ty1.isFailure) "ROLLBACK" else "COMMIT").onCompleteAsync(executionContext) { ty2 ->
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

}
