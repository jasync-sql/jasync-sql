package com.github.jasync.r2dbc.mysql

import com.github.jasync.sql.db.exceptions.ConnectionTimeoutedException
import com.github.jasync.sql.db.exceptions.InsufficientParametersException
import com.github.jasync.sql.db.mysql.MySQLQueryResult
import com.github.jasync.sql.db.mysql.exceptions.MySQLException
import com.github.jasync.sql.db.mysql.exceptions.MysqlErrors
import io.r2dbc.spi.R2dbcBadGrammarException
import io.r2dbc.spi.R2dbcDataIntegrityViolationException
import io.r2dbc.spi.R2dbcPermissionDeniedException
import io.r2dbc.spi.R2dbcRollbackException
import io.r2dbc.spi.R2dbcTimeoutException
import io.r2dbc.spi.R2dbcTransientResourceException
import io.r2dbc.spi.Result
import io.r2dbc.spi.Statement
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import reactor.core.publisher.onErrorMap
import reactor.core.publisher.toFlux
import reactor.core.publisher.toMono
import java.io.IOException
import java.util.function.Supplier
import com.github.jasync.sql.db.Connection as JasyncConnection

internal class JasyncStatement(private val clientSupplier: Supplier<JasyncConnection>, private val sql: String) :
    Statement {

    private val bindings = Bindings()

    private var isPrepared = false
    private var selectLastInsertId: Boolean = false

    private var generatedKeyName: String = "LAST_INSERT_ID"

    override fun returnGeneratedValues(vararg columns: String): Statement {
        if (columns.size == 1) {
            require(columns[0].isNotEmpty()) { "generated value name must not be empty" }
            require(columns[0].indexOf('`') < 0) { "generated value name must not contain backticks" }

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

    override fun bind(identifier: String, value: Any): Statement {
        return bind(identifier.toInt(), value)
    }

    override fun bind(index: Int, value: Any): Statement {
        isPrepared = true
        bindings.current()[index] = value
        return this
    }

    override fun bindNull(identifier: String, type: Class<*>): Statement {
        return bindNull(identifier.toInt(), type)
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

                allParams.concatMap { connection.sendPreparedStatement(sql, it).toMono() }
            } else {
                connection.sendQuery(sql).toMono()
            }
        }
            .map {
                if (selectLastInsertId) {
                    val lastInsertId = (it as MySQLQueryResult).lastInsertId
                    JasyncResult(it.rows, it.rowsAffected, selectLastInsertId, lastInsertId, generatedKeyName)
                } else {
                    JasyncResult(it.rows, it.rowsAffected)
                }
            }
            .onErrorMap(Throwable::class) { throwable ->
                when (throwable) {
                    is ConnectionTimeoutedException -> R2dbcTimeoutException(throwable)
                    is IllegalArgumentException -> throwable
                    is IllegalStateException -> throwable
                    is UnsupportedOperationException -> throwable
                    is IOException -> throwable
                    is MySQLException -> {
                        val errorMessage = throwable.errorMessage
                        when {
                            errorMessage.errorCode == MysqlErrors.ER_DBACCESS_DENIED_ERROR ||
                                errorMessage.errorCode == MysqlErrors.ER_ACCESS_DENIED_ERROR
                            -> R2dbcPermissionDeniedException(
                                errorMessage.errorMessage,
                                errorMessage.sqlState,
                                errorMessage.errorCode,
                                throwable
                            )
                            errorMessage.errorCode == MysqlErrors.ER_DUP_ENTRY -> R2dbcDataIntegrityViolationException(
                                errorMessage.errorMessage,
                                errorMessage.sqlState,
                                errorMessage.errorCode,
                                throwable
                            )
                            errorMessage.errorCode == MysqlErrors.ER_PARSE_ERROR -> R2dbcBadGrammarException(
                                errorMessage.errorMessage,
                                errorMessage.sqlState,
                                errorMessage.errorCode,
                                sql,
                                throwable
                            )
                            errorMessage.errorCode == MysqlErrors.ER_QUERY_TIMEOUT -> R2dbcTimeoutException(
                                errorMessage.errorMessage,
                                errorMessage.sqlState,
                                errorMessage.errorCode,
                                throwable
                            )
                            errorMessage.errorCode == MysqlErrors.ER_XA_RBROLLBACK -> R2dbcRollbackException(
                                errorMessage.errorMessage,
                                errorMessage.sqlState,
                                errorMessage.errorCode,
                                throwable
                            )
                            errorMessage.errorCode == MysqlErrors.ER_LOCK_DEADLOCK ||
                                errorMessage.errorCode == MysqlErrors.ER_LOCK_WAIT_TIMEOUT ||
                                errorMessage.errorCode == MysqlErrors.ER_SERVER_SHUTDOWN
                            -> R2dbcTransientResourceException(
                                errorMessage.errorMessage,
                                errorMessage.sqlState,
                                errorMessage.errorCode,
                                throwable
                            )
                            else -> JasyncDatabaseException(
                                errorMessage.errorMessage,
                                errorMessage.sqlState,
                                errorMessage.errorCode,
                                throwable
                            )
                        }
                    }
                    is InsufficientParametersException -> R2dbcDataIntegrityViolationException(
                        throwable.message,
                        throwable
                    )
                    else -> R2dbcTimeoutException(throwable)
                }
            }
    }
}
