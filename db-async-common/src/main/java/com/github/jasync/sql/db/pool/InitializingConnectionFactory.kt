package com.github.jasync.sql.db.pool

import com.github.jasync.sql.db.ConcreteConnection
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/** A wrapper around [delegate] that runs [initQuery] on every connection when it is created. */
class InitializingConnectionFactory<T : ConcreteConnection>(
    private val initQuery: String,
    private val executionContext: Executor,
    private val delegate: ConnectionFactory<T>
) : ConnectionFactory<T>() {

    override fun create(): CompletableFuture<out T> {
        return delegate.create().thenComposeAsync({ conn ->
            logger.debug { "Initializing new connection with the configured connectionInitializationQuery" }
            conn.sendQuery(initQuery)
                .thenApply { conn }
        }, executionContext)
    }
}
