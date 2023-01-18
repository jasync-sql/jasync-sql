package com.github.jasync.r2dbc.mysql

import com.github.jasync.sql.db.exceptions.ConnectionTimeoutedException
import com.github.jasync.sql.db.exceptions.InsufficientParametersException
import com.github.jasync.sql.db.mysql.MySQLQueryResult
import com.github.jasync.sql.db.mysql.exceptions.MySQLException
import com.github.jasync.sql.db.mysql.exceptions.MysqlErrors
import io.r2dbc.spi.Parameter
import io.r2dbc.spi.R2dbcBadGrammarException
import io.r2dbc.spi.R2dbcDataIntegrityViolationException
import io.r2dbc.spi.R2dbcPermissionDeniedException
import io.r2dbc.spi.R2dbcRollbackException
import io.r2dbc.spi.R2dbcTimeoutException
import io.r2dbc.spi.Result
import io.r2dbc.spi.Statement
import mu.KotlinLogging
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.onErrorMap
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import java.io.IOException
import java.util.function.Supplier
import com.github.jasync.sql.db.Connection as JasyncConnection

private val logger = KotlinLogging.logger {}

internal class JasyncStatement(private val clientSupplier: Supplier<JasyncConnection>, private val sql: String) :
    Statement {

    private val bindings = Bindings()

    private var isPrepared = false
    private var selectLastInsertId: Boolean = false
    private var releasePreparedStatementAfterUse: Boolean = false

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
        logger.trace { "setting selectLastInsertId for $generatedKeyName" }
        selectLastInsertId = true
        return this
    }

    override fun add(): Statement {
        logger.trace { "add() was called" }
        if (isPrepared) {
            bindings.done()
        }
        return this
    }

    override fun bind(identifier: String, value: Any): Statement {
        throw UnsupportedOperationException("named binding is not supported by jasync driver $identifier=$value")
    }

    override fun bind(index: Int, value: Any): Statement {
        logger.trace { "bind $index=$value" }
        isPrepared = true
        bindings.current()[index] = value
        return this
    }

    override fun bindNull(identifier: String, type: Class<*>): Statement {
        throw UnsupportedOperationException("named binding is not supported by jasync driver $identifier=$type")
    }

    override fun bindNull(index: Int, type: Class<*>): Statement {
        logger.trace { "bindNull $index=$type" }
        isPrepared = true
        bindings.current()[index] = null
        return this
    }

    fun releasePreparedStatementAfterUse(): Statement {
        check(isPrepared) { "releasePreparedStatementAfterUse can only be called for prepared statements" }
        logger.trace { "releasePreparedStatementAfterUse() called" }
        releasePreparedStatementAfterUse = true
        return this
    }

    override fun execute(): Publisher<out Result> {
        logger.trace { "execute() called" }
        return Mono.fromSupplier(clientSupplier).flatMapMany { connection ->
            if (isPrepared) {
                val allParams = bindings.all().asSequence().mapIndexed { i, binding ->
                    (0 until binding.size).map {
                        if (it in binding) {
                            mapBindingValue(binding[it])
                        } else {
                            throw IllegalStateException("binding failed with bind index $i and param index $it for query '$sql'")
                        }
                    }
                }.toFlux()
                allParams.concatMap { connection.sendPreparedStatement(sql, it, releasePreparedStatementAfterUse).toMono() }
            } else {
                connection.sendQuery(sql).toMono()
            }
        }
            .map {
                logger.trace { "execute.map $selectLastInsertId" }
                if (selectLastInsertId) {
                    val lastInsertId = (it as MySQLQueryResult).lastInsertId
                    JasyncResult(it.rows, it.rowsAffected, selectLastInsertId, lastInsertId, generatedKeyName)
                } else {
                    JasyncResult(it.rows, it.rowsAffected)
                }
            }
            .onErrorMap(Throwable::class) { throwable ->
                logger.trace { "mapException ${throwable.javaClass}" }
                mapException(throwable)
            }
    }

    private fun mapBindingValue(bindValue: Any?): Any? {
        logger.trace { "mapping bindValue type ${bindValue?.javaClass} $bindValue" }
        return when (bindValue) {
            is Parameter -> bindValue.value
            else -> bindValue
        }
    }

    private fun mapException(throwable: Throwable) = when (throwable) {
        is ConnectionTimeoutedException -> R2dbcTimeoutException(throwable)
        is IllegalArgumentException -> throwable
        is IllegalStateException -> throwable
        is UnsupportedOperationException -> throwable
        is IOException -> throwable
        is MySQLException -> {
            val errorMessage = throwable.errorMessage
            when {
                errorMessage.errorCode == MysqlErrors.ER_DBACCESS_DENIED_ERROR -> R2dbcPermissionDeniedException(
                    errorMessage.errorMessage,
                    errorMessage.sqlState,
                    errorMessage.errorCode,
                    throwable
                )

                errorMessage.errorCode == MysqlErrors.ER_ACCESS_DENIED_ERROR -> R2dbcPermissionDeniedException(
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

                errorMessage.errorCode == 3024 || errorMessage.errorCode == MysqlErrors.ER_QUERY_TIMEOUT -> R2dbcTimeoutException(
                    errorMessage.errorMessage, errorMessage.sqlState, errorMessage.errorCode, throwable
                )

                errorMessage.errorCode == MysqlErrors.ER_XA_RBROLLBACK -> R2dbcRollbackException(
                    errorMessage.errorMessage, errorMessage.sqlState, errorMessage.errorCode, throwable
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

        else -> JasyncDatabaseException("Unknown exception", "UNKOWN", -1, throwable)
    }
}
