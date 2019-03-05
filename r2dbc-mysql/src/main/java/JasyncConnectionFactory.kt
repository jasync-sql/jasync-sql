package com.github.jasync.r2dbc.mysql

import com.github.jasync.sql.db.mysql.pool.MySQLConnectionFactory
import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryMetadata
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import org.reactivestreams.Publisher
import java.util.concurrent.atomic.AtomicReference
import com.github.jasync.sql.db.Connection as JasyncConnection


class JasyncConnectionFactory(private val mySQLConnectionFactory: MySQLConnectionFactory) : ConnectionFactory {

    override fun create(): Publisher<out Connection> {
        return Flowable.defer {
            val ref = AtomicReference<JasyncConnection>()
            Flowable.create<JasyncClientConnection>({ emitter ->
                mySQLConnectionFactory.create().handle { a, t: Throwable? ->
                    if (t == null) {
                        ref.set(a)
                        emitter.onNext(JasyncClientConnection(a))
                        emitter.onComplete()
                    } else {
                        emitter.onError(t)
                    }
                }
            }, BackpressureStrategy.BUFFER).doOnCancel {
                ref.get()?.disconnect()
            }
        }
    }

    override fun getMetadata(): ConnectionFactoryMetadata {
        return Metadata.INSTANCE
    }

    internal enum class Metadata : ConnectionFactoryMetadata {

        INSTANCE;

        override fun getName(): String {
            return "Jasync-MySQL"
        }
    }
}