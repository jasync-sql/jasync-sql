package com.github.jasync.r2dbc.mysql

import com.github.jasync.sql.db.mysql.pool.MySQLConnectionFactory
import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryMetadata
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

class JasyncConnectionFactory(private val mySQLConnectionFactory: MySQLConnectionFactory) : ConnectionFactory {

    override fun create(): Publisher<out Connection> {
        return Mono.defer { mySQLConnectionFactory.create().toMono().map { JasyncClientConnection(it, mySQLConnectionFactory) } }
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
