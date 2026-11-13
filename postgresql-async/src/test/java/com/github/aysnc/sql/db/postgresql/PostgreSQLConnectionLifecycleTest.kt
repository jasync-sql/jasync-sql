package com.github.aysnc.sql.db.postgresql

import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference

class PostgreSQLConnectionLifecycleTest {

    @Test
    fun `disconnect should fail and clear a pending query promise`() {
        val connection = PostgreSQLConnection()
        val pendingQuery = CompletableFuture<QueryResult>()
        connection.setPendingQueryPromise(pendingQuery)

        connection.disconnect()

        assertThat(connection.isQuerying()).isFalse()
        assertThat(pendingQuery.isCompletedExceptionally).isTrue()
    }

    @Test
    fun `query timeout path should fail and clear a pending query promise`() {
        val connection = PostgreSQLConnection()
        val pendingQuery = CompletableFuture<QueryResult>()
        connection.setPendingQueryPromise(pendingQuery)

        invokeOnQueryTimeout(connection)

        assertThat(connection.isQuerying()).isFalse()
        assertThat(pendingQuery.isCompletedExceptionally).isTrue()
    }

    @Test
    fun `channel disconnect should fail and clear a pending query promise`() {
        val connection = PostgreSQLConnection()
        val pendingQuery = CompletableFuture<QueryResult>()
        connection.setPendingQueryPromise(pendingQuery)

        connection.onDisconnect()

        assertThat(connection.isQuerying()).isFalse()
        assertThat(pendingQuery.isCompletedExceptionally).isTrue()
    }

    private fun PostgreSQLConnection.setPendingQueryPromise(promise: CompletableFuture<QueryResult>) {
        val field = PostgreSQLConnection::class.java.getDeclaredField("queryPromiseReference")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val reference = field.get(this) as AtomicReference<Optional<CompletableFuture<QueryResult>>>
        reference.set(Optional.of(promise))
    }

    private fun invokeOnQueryTimeout(connection: PostgreSQLConnection) {
        val method = PostgreSQLConnection::class.java.getDeclaredMethod("onQueryTimeout")
        method.isAccessible = true
        method.invoke(connection)
    }
}
