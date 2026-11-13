package com.github.aysnc.sql.db.postgresql

import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.exceptions.DatabaseException
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import com.github.jasync.sql.db.postgresql.codec.PostgreSQLConnectionDelegate
import com.github.jasync.sql.db.postgresql.codec.PostgreSQLConnectionHandler
import com.github.jasync.sql.db.postgresql.column.PostgreSQLColumnEncoderRegistry
import io.mockk.mockk
import io.mockk.verify
import io.netty.channel.ChannelHandlerContext
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class PostgreSQLConnectionLifecycleTest {

    @Test
    fun `disconnect should fail pending promises and complete disconnect future`() {
        val connection = PostgreSQLConnection()
        val pendingQuery = CompletableFuture<QueryResult>()
        val pendingCloseStatement = CompletableFuture<Any>()
        connection.setAtomicOptionalField("queryPromiseReference", pendingQuery)
        connection.setAtomicOptionalField("closeStatementReference", pendingCloseStatement)

        val disconnectFuture = connection.disconnect()

        assertThat(disconnectFuture.get(5, TimeUnit.SECONDS)).isSameAs(connection)
        assertThat(connection.isConnected()).isFalse()
        assertThat(connection.isQuerying()).isFalse()
        assertThat(pendingQuery.isCompletedExceptionally).isTrue()
        assertThat(pendingCloseStatement.isCompletedExceptionally).isTrue()
    }

    @Test
    fun `onDisconnect should fail connectionFuture and pending promises`() {
        val connection = PostgreSQLConnection()
        val pendingQuery = CompletableFuture<QueryResult>()
        val pendingCloseStatement = CompletableFuture<Any>()
        connection.setAtomicOptionalField("queryPromiseReference", pendingQuery)
        connection.setAtomicOptionalField("closeStatementReference", pendingCloseStatement)

        connection.onDisconnect()

        assertThat(connection.isQuerying()).isFalse()
        assertThat(pendingQuery.isCompletedExceptionally).isTrue()
        assertThat(pendingCloseStatement.isCompletedExceptionally).isTrue()

        val connectionFuture = connection.getConnectionFuture()
        assertThatThrownBy {
            connectionFuture.get(5, TimeUnit.SECONDS)
        }
            .isInstanceOf(ExecutionException::class.java)
            .hasCauseInstanceOf(DatabaseException::class.java)
    }

    @Test
    fun `channelInactive should mark handler disconnected and notify delegate`() {
        val delegate = mockk<PostgreSQLConnectionDelegate>(relaxed = true)
        val configuration = Configuration("test")
        val handler = PostgreSQLConnectionHandler(
            configuration,
            PostgreSQLColumnEncoderRegistry.Instance,
            delegate,
            configuration.eventLoopGroup,
            configuration.executionContext
        )
        val ctx = mockk<ChannelHandlerContext>(relaxed = true)

        handler.channelInactive(ctx)

        assertThat(handler.isConnected()).isFalse()
        verify { delegate.onDisconnect() }
    }

    private fun <T> PostgreSQLConnection.setAtomicOptionalField(fieldName: String, promise: CompletableFuture<T>) {
        val field = PostgreSQLConnection::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val reference = field.get(this) as AtomicReference<Optional<CompletableFuture<T>>>
        reference.set(Optional.of(promise))
    }

    private fun PostgreSQLConnection.getConnectionFuture(): CompletableFuture<*> {
        val field = PostgreSQLConnection::class.java.getDeclaredField("connectionFuture")
        field.isAccessible = true
        return field.get(this) as CompletableFuture<*>
    }
}
