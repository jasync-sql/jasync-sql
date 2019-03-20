package com.github.jasync.r2dbc.mysql


import io.r2dbc.spi.Batch
import io.r2dbc.spi.Connection
import io.r2dbc.spi.IsolationLevel
import io.r2dbc.spi.Statement
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.util.function.Supplier
import com.github.jasync.sql.db.Connection as JasyncConnection

class JasyncClientConnection(private val jasyncConnection: JasyncConnection) : Connection, Supplier<JasyncConnection> {

    override fun beginTransaction(): Publisher<Void> {
        return executeVoid("START TRANSACTION")
    }

    override fun commitTransaction(): Publisher<Void> {
        return executeVoid("COMMIT")
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
    }

    override fun createStatement(sql: String): Statement {
        return SimpleStatement(this, sql)
    }

    override fun close(): Publisher<Void> {
        return Mono.defer { jasyncConnection.disconnect().toMono().then() };
    }

    override fun createBatch(): Batch {
        TODO("not implemented")
    }

    override fun get(): JasyncConnection {
        return jasyncConnection
    }

    private fun executeVoid(sql: String) =
            Mono.defer({ jasyncConnection.sendQuery(sql).toMono().then() })

    private fun assertValidSavepointName(name: String) {
        if (name.isEmpty()) {
            throw IllegalArgumentException("Savepoint name is empty")
        }
        if (name.indexOf('`') != -1) {
            throw IllegalArgumentException("Savepoint name must not contain backticks")
        }
    }
}
