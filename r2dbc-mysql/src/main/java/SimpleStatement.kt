package com.github.jasync.r2dbc.mysql

import com.github.jasync.sql.db.QueryResult
import io.r2dbc.spi.Result
import io.r2dbc.spi.Statement
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.util.function.Supplier
import com.github.jasync.sql.db.Connection as JasyncConnection

class SimpleStatement(private val clientSupplier: Supplier<JasyncConnection>, private val sql: String) : Statement {

    private var isPrepared = false
    private var params: MutableMap<Int, Any?> = mutableMapOf()
    private var selectLastInsertId: Boolean = false

    override fun add(): Statement {
        TODO("not implemented")
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

    override fun returnGeneratedValues(vararg columns: String): Statement {
        if (columns.any { !it.equals("LAST_INSERT_ID", true) }) {
            throw IllegalArgumentException("MySQL only support return special generated value that is is 'LAST_INSERT_ID'")
        }

        if (sql.indexOf("INSERT", ignoreCase = true) <= 0) {
            throw IllegalStateException("Statement must be a INSERT command when includes 'LAST_INSERT_ID'")
        }

        selectLastInsertId = true

        return this
    }

    override fun execute(): Publisher<out Result> {
        val jasyncConnection = clientSupplier.get()
        val queried = queryExecute(jasyncConnection)

        if (selectLastInsertId) {
            return queried.flatMap { result ->
                if (selectLastInsertId) {
                    jasyncConnection.sendQuery("SELECT LAST_INSERT_ID()")
                        .toMono()
                        .map { JaysncResult(it.rows, result.rowsAffected) }
                } else {
                    Mono.empty()
                }
            }
        }

        return queried.map { JaysncResult(it.rows, it.rowsAffected) }
    }

    private fun queryExecute(jasyncConnection: JasyncConnection): Mono<QueryResult> {
        return Mono.defer({
            val r = if (isPrepared) {
                val preparedParams = mutableListOf<Any?>()
                for (i in 0 until params.size) {
                    if (params.containsKey(i)) {
                        preparedParams += params[i]
                    } else {
                        throw IllegalStateException("failed to bind param with index $i for query '${this.sql}'")
                    }
                }
                jasyncConnection.sendPreparedStatement(this.sql, preparedParams)
            } else {
                jasyncConnection.sendQuery(this.sql)
            }
            r.toMono()
        })
    }
}
