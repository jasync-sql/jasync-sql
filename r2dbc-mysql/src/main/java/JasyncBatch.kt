package com.github.jasync.r2dbc.mysql

import com.github.jasync.sql.db.Connection
import io.r2dbc.spi.Batch
import io.r2dbc.spi.Result
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.util.function.Supplier

/**
 * An implementation of [Batch] for executing a collection of statements in a FAKE batch against at MySQL server.
 */
class JasyncBatch(private val clientSupplier: Supplier<Connection>) : Batch {

    private val statements: MutableList<String> = arrayListOf()

    override fun add(sql: String): Batch {
        statements += sql
        return this
    }

    /**
     * Note: [Batch] no need support `returnGeneratedValues`, so just use `0` for last inserted ID.
     */
    override fun execute(): Publisher<out Result> = Mono.fromSupplier(clientSupplier).flatMapMany { connection ->
        Flux.fromIterable(statements)
            .concatMap { sql -> connection.sendQuery(sql).toMono().map { JasyncResult(it.rows, it.rowsAffected) } }
    }
}
