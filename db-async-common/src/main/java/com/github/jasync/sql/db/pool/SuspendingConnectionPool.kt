package com.github.jasync.sql.db.pool

import com.github.jasync.sql.db.ConcreteConnection
import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.SuspendingConnection
import com.github.jasync.sql.db.asSuspending
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future

fun <T : ConcreteConnection> ConnectionPool<T>.asSuspending(): SuspendingConnectionPool<T> =
    SuspendingConnectionPool(this)

class SuspendingConnectionPool<T : ConcreteConnection>(
    val connectionPool: ConnectionPool<T>
) : SuspendingConnection {

    suspend fun <A> use(f: suspend (SuspendingConnection) -> A): A =
        connectionPool.use { concreteConnection ->
            CoroutineScope(Job() + connectionPool.configuration.coroutineDispatcher).future {
                f(concreteConnection.asSuspending)
            }
        }.await()

    override suspend fun disconnect(): Connection = connectionPool.disconnect().await()

    override suspend fun connect(): Connection = connectionPool.connect().await()

    override suspend fun sendQuery(query: String): QueryResult = use { suspendingConnection ->
        suspendingConnection.sendQuery(query)
    }

    override suspend fun sendPreparedStatement(
        query: String,
        values: List<Any?>,
        release: Boolean
    ): QueryResult = use { suspendingConnection ->
        suspendingConnection.sendPreparedStatement(query, values, release)
    }

    override suspend fun sendPreparedStatement(
        query: String,
        values: List<Any?>
    ): QueryResult = use { suspendingConnection ->
        suspendingConnection.sendPreparedStatement(query, values)
    }

    override suspend fun sendPreparedStatement(
        query: String
    ): QueryResult = use { suspendingConnection ->
        suspendingConnection.sendPreparedStatement(query)
    }

    override suspend fun <A> inTransaction(
        f: suspend (SuspendingConnection) -> A
    ): A = use { suspendingConnection ->
        suspendingConnection.inTransaction(f)
    }
}
