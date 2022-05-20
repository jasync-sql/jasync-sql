package com.github.jasync.r2dbc.mysql

import com.github.jasync.sql.db.Connection as JasyncConnection
import com.github.jasync.sql.db.mysql.MySQLConnection
import com.github.jasync.sql.db.mysql.pool.MySQLConnectionFactory
import com.github.jasync.sql.db.util.map
import io.r2dbc.spi.Batch
import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionMetadata
import io.r2dbc.spi.IsolationLevel
import io.r2dbc.spi.Statement
import io.r2dbc.spi.ValidationDepth
import java.util.function.Supplier
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.time.Duration

class JasyncClientConnection(
    private val jasyncConnection: com.github.jasync.sql.db.Connection,
    private val mySQLConnectionFactory: MySQLConnectionFactory
) : Connection, Supplier<JasyncConnection> {

    private var isolationLevel: IsolationLevel = IsolationLevel.REPEATABLE_READ

    override fun validate(depth: ValidationDepth): Publisher<Boolean> {
        return when (depth) {
            ValidationDepth.LOCAL -> mySQLConnectionFactory.validate(jasyncConnection as MySQLConnection).isSuccess.toMono()
            ValidationDepth.REMOTE -> Mono.defer {
                mySQLConnectionFactory.test(jasyncConnection as MySQLConnection).map { true }.toMono()
            }
        }
    }

    override fun getMetadata(): ConnectionMetadata {
        return JasyncConnectionMetadata(jasyncConnection)
    }

    override fun beginTransaction(): Publisher<Void> {
        return executeVoid("START TRANSACTION")
    }

    override fun beginTransaction(definition: TransactionDefinition): Publisher<Void> {
        TODO("Not yet implemented")
    }

    override fun commitTransaction(): Publisher<Void> {
        return executeVoid("COMMIT")
    }

    override fun isAutoCommit(): Boolean {
        return (jasyncConnection as MySQLConnection).isAutoCommit()
    }

    override fun setAutoCommit(autoCommit: Boolean): Publisher<Void> {
        return executeVoid("SET AUTOCOMMIT = ${if (autoCommit) 1 else 0}")
    }

    override fun setLockWaitTimeout(timeout: Duration): Publisher<Void> {
        TODO("Not yet implemented")
    }

    override fun setStatementTimeout(timeout: Duration): Publisher<Void> {
        TODO("Not yet implemented")
    }

    override fun createSavepoint(name: String): Publisher<Void> {
        assertValidSavepointName(name)
        return executeVoid("SAVEPOINT `$name`")
    }

    override fun releaseSavepoint(name: String): Publisher<Void> {
        assertValidSavepointName(name)
        return executeVoid("RELEASE SAVEPOINT `$name`")
    }

    override fun rollbackTransactionToSavepoint(name: String): Publisher<Void> {
        assertValidSavepointName(name)
        return executeVoid("ROLLBACK TO SAVEPOINT `$name`")
    }

    override fun rollbackTransaction(): Publisher<Void> {
        return executeVoid("ROLLBACK")
    }

    override fun setTransactionIsolationLevel(isolationLevel: IsolationLevel): Publisher<Void> {
        return executeVoid("SET TRANSACTION ISOLATION LEVEL ${isolationLevel.asSql()}")
                .doOnSuccess { this.isolationLevel = isolationLevel }
    }

    override fun getTransactionIsolationLevel(): IsolationLevel {
        return isolationLevel
    }

    override fun createStatement(sql: String): Statement {
        return JasyncStatement(this, sql)
    }

    override fun close(): Publisher<Void> {
        return Mono.defer { jasyncConnection.disconnect().toMono().then() }
    }

    override fun createBatch(): Batch {
        return JasyncBatch(this)
    }

    override fun get(): JasyncConnection {
        return jasyncConnection
    }

    private fun executeVoid(sql: String) =
            Mono.defer { jasyncConnection.sendQuery(sql).toMono().then() }

    private fun assertValidSavepointName(name: String) {
        if (name.isEmpty()) {
            throw IllegalArgumentException("Savepoint name is empty")
        }
        if (name.indexOf('`') != -1) {
            throw IllegalArgumentException("Savepoint name must not contain backticks")
        }
    }
}
