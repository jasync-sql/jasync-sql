package com.github.jasync.r2dbc.mysql.integ

import com.github.jasync.r2dbc.mysql.JasyncConnectionFactory
import com.github.jasync.sql.db.mysql.MySQLConnection
import com.github.jasync.sql.db.mysql.pool.MySQLConnectionFactory
import com.github.jasync.sql.db.util.FP
import com.github.jasync.sql.db.util.isCompleted
import io.mockk.mockk
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.IsolationLevel
import io.r2dbc.spi.Option
import io.r2dbc.spi.R2dbcBadGrammarException
import io.r2dbc.spi.TransactionDefinition
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.Test
import org.springframework.r2dbc.connection.R2dbcTransactionManager
import org.springframework.r2dbc.connection.TransactionAwareConnectionFactoryProxy
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.support.DefaultTransactionDefinition
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.CompletableFuture
import kotlin.test.assertEquals

class R2dbcTransactionIntegrationTest : R2dbcConnectionHelper() {

    @Test
    fun `verify read committed transaction isolation level`() {
        withConnection { c ->
            executeQuery(c, createTableNumericColumns)
            executeQuery(c, insertTableNumericColumns)
            val mycf = object : MySQLConnectionFactory(mockk()) {
                override fun create(): CompletableFuture<MySQLConnection> {
                    return FP.successful(c)
                }
            }
            val cf = JasyncConnectionFactory(mycf)

            val result = CompletableFuture<Long>()

            Mono.from(cf.create())
                .flatMap { connection ->
                    Mono.from(connection.beginTransaction(ExtendedTransactionDefinition(isolationLevel = IsolationLevel.READ_COMMITTED)))
                        .then(
                            Mono.from(
                                connection
                                    .createStatement("SELECT COUNT(*) FROM numbers")
                                    .execute()
                            )
                        )
                }
                .flatMap { Mono.from(it.map { row, _ -> row.get("COUNT(*)") as Long }) }
                .subscribe(
                    { countResult -> result.complete(countResult) },
                    { throwable -> result.completeExceptionally(throwable) }
                )

            await.untilAsserted {
                assert(result.isCompleted)
                assertEquals(1, result.get())
            }
        }
    }

    @Test
    fun `verify read commited transaction isolation level (spring tx manager)`() {
        val timeout = 3L
        val timeoutConfiguration = getConfiguration().copy(queryTimeout = Duration.ofSeconds(timeout))

        withConfigurableConnection(timeoutConfiguration) { c ->

            executeQuery(c, createTableNumericColumns)
            executeQuery(c, insertTableNumericColumns)

            val mycf = object : MySQLConnectionFactory(mockk()) {
                override fun create(): CompletableFuture<MySQLConnection> {
                    return FP.successful(c)
                }
            }
            val cf = JasyncConnectionFactory(mycf)
            val r2dbcPoolConfig = ConnectionPoolConfiguration.builder()
                .initialSize(5)
                .minIdle(5)
                .connectionFactory(cf)
                .build()

            val r2dbcPool = io.r2dbc.pool.ConnectionPool(r2dbcPoolConfig)
            val tm = R2dbcTransactionManager(r2dbcPool)
            val transactionalOperator = TransactionalOperator.create(
                tm,
                DefaultTransactionDefinition().apply {
                    isolationLevel = org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED
                }
            )

            val tcf = TransactionAwareConnectionFactoryProxy(r2dbcPool)

            val result = CompletableFuture<Long>()

            Mono.from(tcf.create())
                .flatMap { connection ->
                    Mono.from(
                        connection
                            .createStatement("SELECT COUNT(*) FROM numbers")
                            .execute()
                    )
                }
                .flatMap { Mono.from(it.map { row, _ -> row.get("COUNT(*)") as Long }) }
                .`as`(transactionalOperator::transactional)
                .subscribe(
                    { countResult -> result.complete(countResult) },
                    { throwable -> result.completeExceptionally(throwable) }
                )

            await.untilAsserted {
                assert(result.isCompleted)
                assertEquals(1, result.get())
            }
        }
    }

    @Test
    fun `verify default transaction isolation level`() {
        withConnection { c ->
            executeQuery(c, createTableNumericColumns)
            executeQuery(c, insertTableNumericColumns)
            val mycf = object : MySQLConnectionFactory(mockk()) {
                override fun create(): CompletableFuture<MySQLConnection> {
                    return FP.successful(c)
                }
            }
            val cf = JasyncConnectionFactory(mycf)

            val result = CompletableFuture<Long>()

            Mono.from(cf.create())
                .flatMap { connection ->
                    Mono.from(connection.beginTransaction(ExtendedTransactionDefinition(isolationLevel = null)))
                        .then(
                            Mono.from(
                                connection
                                    .createStatement("SELECT COUNT(*) FROM numbers")
                                    .execute()
                            )
                        )
                }
                .flatMap { Mono.from(it.map { row, _ -> row.get("COUNT(*)") as Long }) }
                .subscribe(
                    { countResult -> result.complete(countResult) },
                    { throwable -> result.completeExceptionally(throwable) }
                )

            await.untilAsserted {
                assert(result.isCompleted)
                assertEquals(1, result.get())
            }
        }
    }

    @Test
    fun `rolls back transaction cleanly when query fails inside spring transactional operator`() {
        withConnection { setupConn ->
            executeQuery(
                setupConn,
                "CREATE TABLE IF NOT EXISTS rollback_test_408 (id INT NOT NULL PRIMARY KEY, name VARCHAR(255) NOT NULL)"
            )
            executeQuery(setupConn, "DELETE FROM rollback_test_408")
        }

        val cf = JasyncConnectionFactory(MySQLConnectionFactory(getConfiguration()))
        val r2dbcPool = io.r2dbc.pool.ConnectionPool(
            ConnectionPoolConfiguration.builder()
                .initialSize(1)
                .maxSize(2)
                .connectionFactory(cf)
                .build()
        )
        try {
            val tm = R2dbcTransactionManager(r2dbcPool)
            val transactionalOperator = TransactionalOperator.create(tm)
            val tcf = TransactionAwareConnectionFactoryProxy(r2dbcPool)

            val failure = CompletableFuture<Throwable>()

            Mono.from(tcf.create())
                .flatMap { connection ->
                    Mono.from(
                        connection.createStatement("INSERT INTO rollback_test_408 (id, name) VALUES (101, 'RollbackTarget')")
                            .execute()
                    )
                        .flatMap { Mono.from(it.rowsUpdated) }
                        .then(
                            Mono.from(
                                connection.createStatement("SELECT * FROM nonexistent_table_408")
                                    .execute()
                            )
                        )
                }
                .`as`(transactionalOperator::transactional)
                .subscribe(
                    {},
                    { throwable -> failure.complete(throwable) }
                )

            await.untilAsserted {
                assert(failure.isCompleted)
                assertThat(failure.get()).hasRootCauseInstanceOf(com.github.jasync.sql.db.mysql.exceptions.MySQLException::class.java)
                assertThat(failure.get()).isInstanceOfAny(
                    R2dbcBadGrammarException::class.java,
                    org.springframework.dao.DataAccessException::class.java
                )
                assertEquals(0, r2dbcPool.metrics.get().acquiredSize())
            }

            withConnection { verifyConn ->
                val rows = executeQuery(verifyConn, "SELECT COUNT(*) AS cnt FROM rollback_test_408 WHERE id = 101").rows
                assertEquals(0L, rows[0]["cnt"])
            }
        } finally {
            r2dbcPool.disposeLater().block()
            withConnection { cleanupConn ->
                executeQuery(cleanupConn, "DROP TABLE IF EXISTS rollback_test_408")
            }
        }
    }

    private class ExtendedTransactionDefinition constructor(
        private val transactionName: String? = null,
        private val readOnly: Boolean = false,
        private val isolationLevel: IsolationLevel? = null,
        private val lockWaitTimeout: Duration = Duration.ofMillis(0)
    ) : TransactionDefinition {

        override fun <T> getAttribute(option: Option<T>): T {
            return doGetValue(option) as T
        }

        private fun doGetValue(option: Option<*>): Any? {
            if (TransactionDefinition.ISOLATION_LEVEL == option) {
                return this.isolationLevel
            }
            if (TransactionDefinition.NAME == option) {
                return this.transactionName
            }
            if (TransactionDefinition.READ_ONLY == option) {
                return this.readOnly
            }
            return if (TransactionDefinition.LOCK_WAIT_TIMEOUT == option && !this.lockWaitTimeout.isZero) {
                this.lockWaitTimeout
            } else {
                null
            }
        }
    }
}
