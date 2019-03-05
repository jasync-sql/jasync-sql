package com.github.jasync.r2dbc.mysql

import io.r2dbc.spi.Result
import io.r2dbc.spi.Statement
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import org.reactivestreams.Publisher
import java.util.function.Supplier
import com.github.jasync.sql.db.Connection as JasyncConnection

class SimpleStatement(private val clientSupplier: Supplier<JasyncConnection>, private val sql: String) : Statement {

    override fun add(): Statement {
        TODO("not implemented")
    }

    override fun bind(identifier: Any, value: Any): Statement {
        TODO("not implemented")
    }

    override fun bind(index: Int, value: Any): Statement {
        TODO("not implemented")
    }

    override fun bindNull(identifier: Any, type: Class<*>): Statement {
        TODO("not implemented")
    }

    override fun bindNull(index: Int, type: Class<*>): Statement {
        TODO("not implemented")
    }

    override fun execute(): Publisher<out Result> {
        return Flowable.create({ emitter ->
            val jasyncConnection = clientSupplier.get()
            jasyncConnection.sendQuery(this.sql).handle { a, t: Throwable? ->
                if (t == null) {
                    val result = a.rows
                    emitter.onNext(JaysncResult(result))
                    emitter.onComplete()
                } else {
                    emitter.onError(t)
                }
            }
        }, BackpressureStrategy.BUFFER)
    }
}
