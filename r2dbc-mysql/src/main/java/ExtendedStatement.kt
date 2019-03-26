package com.github.jasync.r2dbc.mysql

import com.github.jasync.sql.db.QueryResult
import io.r2dbc.spi.Result
import io.r2dbc.spi.Statement
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import reactor.core.publisher.toMono
import java.util.function.Supplier
import com.github.jasync.sql.db.Connection as JasyncConnection

internal class ExtendedStatement(private val clientSupplier: Supplier<JasyncConnection>, private val sql: String) :
    Statement {

    private val bindings = Bindings()

    private var isPrepared = false
    private var selectLastInsertId: Boolean = false

    private var generatedKeyName: String = "LAST_INSERT_ID"

    override fun returnGeneratedValues(vararg columns: String): Statement {
        if (columns.size == 1) {
            generatedKeyName = columns[0]
        }
        if (columns.size > 1) {
            throw IllegalArgumentException("MySQL only supports a single generated value")
        }
        selectLastInsertId = true
        return this
    }

    override fun add(): Statement {
        if (isPrepared) {
            bindings.done()
        }

        return this
    }

    override fun bind(identifier: Any, value: Any): Statement {
        if (identifier is String) {
            return bind(identifier.toInt(), value)
        } else {
            throw IllegalArgumentException("cant bind identifier $identifier with value '$value'")
        }
    }

    override fun bind(index: Int, value: Any): Statement {
        isPrepared = true
        bindings.current()[index] = value
        return this
    }

    override fun bindNull(identifier: Any, type: Class<*>): Statement {
        if (identifier is String) {
            return bindNull(identifier.toInt(), type)
        } else {
            throw IllegalArgumentException("cant bind null identifier $identifier")
        }
    }

    override fun bindNull(index: Int, type: Class<*>): Statement {
        isPrepared = true
        bindings.current()[index] = null
        return this
    }

    override fun execute(): Publisher<out Result> {
        return Mono.fromSupplier(clientSupplier).flatMapMany { connection ->
            if (isPrepared) {
                val allParams = bindings.all().asSequence().mapIndexed { i, binding ->
                    (0 until binding.size).map {
                        if (it in binding) {
                            binding[it]
                        } else {
                            throw IllegalStateException("binding failed with bind index $i and param index $it for query '$sql'")
                        }
                    }
                }.toFlux()

                allParams.concatMap { extraGeneratedQuery(connection, connection.sendPreparedStatement(sql, it).toMono()) }
            } else {
                extraGeneratedQuery(connection, connection.sendQuery(sql).toMono())
            }
        }.map { JaysncResult(it.rows, it.rowsAffected) }
    }

    private fun extraGeneratedQuery(connection: JasyncConnection, result: Mono<QueryResult>): Mono<QueryResult> {
        return if (selectLastInsertId) {
            result.flatMap { connection.sendQuery("SELECT LAST_INSERT_ID() AS $generatedKeyName").toMono() }
        } else {
            result
        }
    }
}
