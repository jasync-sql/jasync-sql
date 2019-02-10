package com.github.jasync.sql.db

import com.github.jasync.sql.db.util.FP
import com.github.jasync.sql.db.util.flatMapTry
import com.github.jasync.sql.db.util.map
import mu.KotlinLogging
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

private val logger = KotlinLogging.logger {}

abstract class ConcreteConnectionBase(
    val configuration: Configuration,
    val executionContext: Executor
) : ConcreteConnection {

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
