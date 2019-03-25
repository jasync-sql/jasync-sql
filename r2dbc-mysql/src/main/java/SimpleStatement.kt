package com.github.jasync.r2dbc.mysql

import com.github.jasync.sql.db.QueryResult
import io.r2dbc.spi.Result
import io.r2dbc.spi.Statement
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier
import com.github.jasync.sql.db.Connection as JasyncConnection

class SimpleStatement(private val clientSupplier: Supplier<JasyncConnection>, private val sql: String) : Statement {

    private var isPrepared = false
    private var params: MutableMap<Int, Any?> = mutableMapOf()
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
        throw UnsupportedOperationException("add is not supported")
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
        params[index] = value
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
        params[index] = null
        return this
    }

    override fun execute(): Publisher<out Result> {
        return Mono.fromSupplier(clientSupplier).flatMap { connection ->
            val queried = queryExecute(connection).toMono()
            if (selectLastInsertId) {
                queried.flatMap { result ->
                    connection.sendQuery("SELECT LAST_INSERT_ID() AS $generatedKeyName")
                        .toMono()
                }
            } else {
                queried
            }
        }.map { JaysncResult(it.rows, it.rowsAffected) }
    }

    private fun queryExecute(connection: JasyncConnection): CompletableFuture<QueryResult> {
        return if (isPrepared) {
            val preparedParams = mutableListOf<Any?>()
            for (i in 0 until params.size) {
                if (params.containsKey(i)) {
                    preparedParams += params[i]
                } else {
                    throw IllegalStateException("failed to bind param with index $i for query '$sql'")
                }
            }
            connection.sendPreparedStatement(sql, preparedParams)
        } else {
            connection.sendQuery(sql)
        }
    }


}

