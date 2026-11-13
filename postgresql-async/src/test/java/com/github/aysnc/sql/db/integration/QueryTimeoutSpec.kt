package com.github.aysnc.sql.db.integration

import com.github.aysnc.sql.db.verifyException
import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.ConnectionPoolConfiguration
import com.github.jasync.sql.db.asSuspending
import com.github.jasync.sql.db.exceptions.ConnectionStillRunningQueryException
import com.github.jasync.sql.db.exceptions.ConnectionTimeoutedException
import com.github.jasync.sql.db.pool.ConnectionPool
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import com.github.jasync.sql.db.postgresql.pool.PostgreSQLConnectionFactory
import com.github.jasync.sql.db.util.ExecutorServiceUtils
import com.github.jasync.sql.db.util.flatMapAsync
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.Test
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class QueryTimeoutSpec : DatabaseTestHelper() {

    @Test
    fun `Simple query with short timeout`() {
        withConfigurablePool(shortTimeoutConfiguration()) { pool ->
            val connection = pool.take().get(10, TimeUnit.SECONDS)
            assertThat(connection.isTimeout()).isFalse()
            assertThat(connection.isConnected()).isTrue()

            val queryResultFuture = connection.sendQuery("SELECT pg_sleep(2)")
            verifyException(ExecutionException::class.java, TimeoutException::class.java) {
                queryResultFuture.get(10, TimeUnit.SECONDS)
            }

            assertThat(connection.isTimeout()).isTrue()
            assertThat(connection.isConnected()).isFalse()
            assertThat(connection.isQuerying()).isFalse()

            assertThatThrownBy {
                connection.sendQuery("ROLLBACK")
            }
                .isInstanceOf(IllegalStateException::class.java)
                .isNotInstanceOf(ConnectionStillRunningQueryException::class.java)

            verifyException(ExecutionException::class.java, ConnectionTimeoutedException::class.java) {
                pool.giveBack(connection).get(10, TimeUnit.SECONDS)
            }
            await.untilCallTo { pool.idleConnectionsCount } matches { it == 0 }
        }
    }

    @Test
    fun `Simple query with short timeout directly on pool`() {
        withConfigurablePool(shortTimeoutConfiguration()) { pool ->
            val queryResultFuture = pool.sendQuery("SELECT pg_sleep(2)")
            verifyException(ExecutionException::class.java, TimeoutException::class.java) {
                queryResultFuture.get(10, TimeUnit.SECONDS)
            }
            await.untilCallTo { pool.idleConnectionsCount } matches { it == 0 }
        }
    }

    @Test
    fun `inTransaction with query timeout surfaces TimeoutException and rolls back on disconnect`() {
        val tableName = "tx_timeout_${UUID.randomUUID().toString().replace("-", "")}"
        withHandler { setupConn ->
            executeDdl(setupConn, "CREATE TABLE $tableName (x integer PRIMARY KEY)")
        }
        try {
            withConfigurablePool(shortTimeoutConfiguration()) { pool ->
                verifyException(ExecutionException::class.java, TimeoutException::class.java) {
                    pool.inTransaction { conn ->
                        conn.sendQuery("INSERT INTO $tableName VALUES (1)").flatMapAsync(ExecutorServiceUtils.CommonPool) {
                            conn.sendQuery("SELECT pg_sleep(2)")
                        }
                    }.get(10, TimeUnit.SECONDS)
                }

                verifyException(ExecutionException::class.java, TimeoutException::class.java) {
                    pool.inTransaction { conn ->
                        conn.sendPreparedStatement("INSERT INTO $tableName VALUES (2)", emptyList(), true)
                            .flatMapAsync(ExecutorServiceUtils.CommonPool) {
                                conn.sendPreparedStatement("SELECT pg_sleep(2)", emptyList(), true)
                            }
                    }.get(10, TimeUnit.SECONDS)
                }
            }

            withHandler { checkConn ->
                val rows = executeQuery(checkConn, "SELECT x FROM $tableName").rows
                assertThat(rows.size).isEqualTo(0)
            }
        } finally {
            withHandler { cleanupConn ->
                executeDdl(cleanupConn, "DROP TABLE IF EXISTS $tableName")
            }
        }
    }

    @Test
    fun `suspending inTransaction with query timeout and caller timeout surfaces timeout exception`() {
        val tableName = "tx_susp_timeout_${UUID.randomUUID().toString().replace("-", "")}"
        withHandler { setupConn ->
            executeDdl(setupConn, "CREATE TABLE $tableName (x integer PRIMARY KEY)")
        }
        try {
            withConfigurablePool(shortTimeoutConfiguration()) { pool ->
                assertThatThrownBy {
                    runBlocking {
                        pool.asSuspending.inTransaction { tx ->
                            tx.sendQuery("INSERT INTO $tableName VALUES (1)")
                            tx.sendQuery("SELECT pg_sleep(2)")
                        }
                    }
                }.isInstanceOf(TimeoutException::class.java)
            }

            withConfigurablePool(longTimeoutConfiguration()) { pool ->
                assertThatThrownBy {
                    runBlocking {
                        pool.asSuspending.inTransaction { tx ->
                            tx.sendQuery("INSERT INTO $tableName VALUES (2)")
                            withTimeout(150) {
                                tx.sendQuery("SELECT pg_sleep(2)")
                            }
                        }
                    }
                }.isInstanceOf(TimeoutCancellationException::class.java)

                val recoveryRows = pool.sendQuery("SELECT x FROM $tableName").get(10, TimeUnit.SECONDS).rows
                assertThat(recoveryRows.size).isEqualTo(0)
            }
        } finally {
            withHandler { cleanupConn ->
                executeDdl(cleanupConn, "DROP TABLE IF EXISTS $tableName")
            }
        }
    }

    private fun shortTimeoutConfiguration() =
        ContainerHelper.defaultConfiguration.copy(queryTimeout = Duration.ofMillis(150))

    private fun longTimeoutConfiguration() =
        ContainerHelper.defaultConfiguration.copy(queryTimeout = Duration.ofSeconds(5))

    private fun <T> withConfigurablePool(
        configuration: Configuration,
        fn: (ConnectionPool<PostgreSQLConnection>) -> T
    ): T {
        val poolConfiguration = ConnectionPoolConfiguration(
            host = configuration.host,
            port = configuration.port,
            database = configuration.database,
            username = configuration.username,
            password = configuration.password,
            maxActiveConnections = 2,
            maxIdleTime = 60_000,
            maxPendingQueries = 10,
            queryTimeout = configuration.queryTimeout?.toMillis()
        )
        val pool = ConnectionPool(PostgreSQLConnectionFactory(configuration), poolConfiguration)
        try {
            return fn(pool)
        } finally {
            pool.disconnect().get(10, TimeUnit.SECONDS)
        }
    }
}
