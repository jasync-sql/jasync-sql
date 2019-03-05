package com.github.jasync.r2dbc.mysql


import io.r2dbc.spi.Batch
import io.r2dbc.spi.Connection
import io.r2dbc.spi.IsolationLevel
import io.r2dbc.spi.Statement
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import org.reactivestreams.Publisher
import java.util.function.Supplier
import com.github.jasync.sql.db.Connection as JasyncConnection

class JasyncClientConnection(private val jasyncConnection: JasyncConnection) : Connection, Supplier<JasyncConnection> {
    override fun rollbackTransactionToSavepoint(name: String): Publisher<Void> {
        TODO("not implemented")
    }

    override fun rollbackTransaction(): Publisher<Void> {
        TODO("not implemented")
    }

    override fun setTransactionIsolationLevel(isolationLevel: IsolationLevel): Publisher<Void> {
        TODO("not implemented")
    }

    override fun commitTransaction(): Publisher<Void> {
        TODO("not implemented")
    }

    override fun createSavepoint(name: String): Publisher<Void> {
        TODO("not implemented")
    }

    override fun releaseSavepoint(name: String): Publisher<Void> {
        TODO("not implemented")
    }

    override fun beginTransaction(): Publisher<Void> {
        TODO("not implemented")
    }

    override fun createStatement(sql: String): Statement {
        return SimpleStatement(this, sql)
    }

    override fun close(): Publisher<Void> {
        return Flowable.create({ emitter ->
            jasyncConnection.disconnect().handle { _, t: Throwable? ->
                if (t == null) {
                    emitter.onComplete()
                } else {
                    emitter.onError(t)
                }
            }
        }, BackpressureStrategy.BUFFER)
    }

    override fun createBatch(): Batch {
        TODO("not implemented")
    }

    override fun get(): JasyncConnection {
        return jasyncConnection
    }
}