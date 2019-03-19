package com.github.jasync.r2dbc.mysql

import io.r2dbc.spi.Result
import io.r2dbc.spi.Statement
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import org.reactivestreams.Publisher
import java.util.function.Supplier
import com.github.jasync.sql.db.Connection as JasyncConnection

class SimpleStatement(private val clientSupplier: Supplier<JasyncConnection>, private val sql: String) : Statement {

    private var isPrepared = false
    private var params: MutableMap<Int, Any?> = mutableMapOf()

    override fun add(): Statement {
        TODO("not implemented")
    }

    override fun bind(identifier: Any, value: Any): Statement {
        TODO("not implemented")
    }

    override fun bind(index: Int, value: Any): Statement {
        isPrepared = true
        params[index] = value
        return this
    }

    override fun bindNull(identifier: Any, type: Class<*>): Statement {
        TODO("not implemented")
    }

    override fun bindNull(index: Int, type: Class<*>): Statement {
        isPrepared = true
        params[index] = null
        return this
    }

    override fun execute(): Publisher<out Result> {
        return Flowable.create({ emitter ->
            val jasyncConnection = clientSupplier.get()
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
            r.handle { a, t: Throwable? ->
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
