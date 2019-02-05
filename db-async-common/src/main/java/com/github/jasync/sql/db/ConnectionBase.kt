package com.github.jasync.sql.db

import com.github.jasync.sql.db.util.FP
import com.github.jasync.sql.db.util.Failure
import com.github.jasync.sql.db.util.Success
import com.github.jasync.sql.db.util.flatMapTry
import com.github.jasync.sql.db.util.map
import com.github.jasync.sql.db.util.onComplete
import mu.KotlinLogging
import java.util.concurrent.CompletableFuture

private val logger = KotlinLogging.logger {}

abstract class ConnectionBase(val configuration: Configuration): Connection {

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

    protected fun wrapQueryWithListeners(
        query: String,
        fn: () -> CompletableFuture<QueryResult>
    ): CompletableFuture<QueryResult> {
        if (configuration.listeners.isEmpty()) {
            return fn()
        }
        val resolvedListeners = configuration.listeners.map { it.get() }
        for (listener in resolvedListeners) {
            try {
                listener.onQuery(query)
            } catch (e: Exception) {
                logger.warn(e) { "failed listener onQuery($query)" }
            }
        }
        return fn().onComplete { r ->
            when (r) {
                is Success -> {
                    for (listener in resolvedListeners) {
                        try {
                            listener.onQueryComplete(r.value)
                        } catch (e: Exception) {
                            logger.warn(e) { "failed listener onQueryComplete(${r.value})" }
                        }
                    }
                }
                is Failure -> {
                    for (listener in resolvedListeners) {
                        try {
                            listener.onQueryError(r.exception)
                        } catch (e: Exception) {
                            logger.warn(e) { "failed listener onQueryError(${r.exception})" }
                        }
                    }
                }
            }
        }


    }
    protected fun wrapPreparedStatementWithListeners(
        query: String,
        values: List<Any?>,
        fn: () -> CompletableFuture<QueryResult>
    ): CompletableFuture<QueryResult> {
        if (configuration.listeners.isEmpty()) {
            return fn()
        }
        val resolvedListeners = configuration.listeners.map { it.get() }
        for (listener in resolvedListeners) {
            try {
                listener.onPreparedStatement(query, values)
            } catch (e: Exception) {
                logger.warn(e) { "failed listener onPreparedStatement($query, $values)" }
            }
        }
        return fn().onComplete { r ->
            when (r) {
                is Success -> {
                    for (listener in resolvedListeners) {
                        try {
                            listener.onPreparedStatementComplete(r.value)
                        } catch (e: Exception) {
                            logger.warn(e) { "failed listener onPreparedStatementComplete(${r.value})" }
                        }
                    }
                }
                is Failure -> {
                    for (listener in resolvedListeners) {
                        try {
                            listener.onPreparedStatementError(r.exception)
                        } catch (e: Exception) {
                            logger.warn(e) { "failed listener onPreparedStatementError() - ${r.exception}" }
                        }
                    }
                }
            }
        }


    }
}