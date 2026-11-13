package com.github.jasync.sql.db.mysql

import com.github.jasync.sql.db.asSuspending
import com.github.jasync.sql.db.exceptions.ConnectionTimeoutedException
import com.github.jasync.sql.db.util.ExecutorServiceUtils
import com.github.jasync.sql.db.util.flatMapAsync
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.Test
import java.time.Duration
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class QueryTimeoutSpec : ConnectionHelper() {

    @Test
    fun `Simple query with short timeout`() {
        withConfigurablePool(shortTimeoutConfiguration()) { pool ->
            val connection = pool.take().get(10, TimeUnit.SECONDS)
            assertThat(connection.isTimeout()).isEqualTo(false)
            assertThat(connection.isConnected()).isEqualTo(true)
            val queryResultFuture = connection.sendQuery("select sleep(100)")
            verifyException(ExecutionException::class.java, TimeoutException::class.java) {
                queryResultFuture.get(10, TimeUnit.SECONDS)
            }
            await.untilCallTo { connection.isTimeout() } matches { it == true }
            verifyException(ExecutionException::class.java, ConnectionTimeoutedException::class.java) {
                pool.giveBack(connection).get(10, TimeUnit.SECONDS)
            }
            await.untilCallTo { pool.idleConnectionsCount } matches { it == 0 } // connection removed from pool
        }
    }

    @Test
    fun `Simple query with short timeout directly on pool`() {
        withConfigurablePool(shortTimeoutConfiguration()) { pool ->
            val queryResultFuture = pool.sendQuery("select sleep(100)")
            verifyException(ExecutionException::class.java, TimeoutException::class.java) {
                queryResultFuture.get(10, TimeUnit.SECONDS)
            }
            await.untilCallTo { pool.idleConnectionsCount } matches { it == 0 } // connection removed from pool
        }
    }

    @Test
    fun `Simple query with 5 sec timeout`() {
        withConfigurablePool(longTimeoutConfiguration()) { pool ->
            val connection = pool.take().get(10, TimeUnit.SECONDS)
            assertThat(connection.isTimeout()).isEqualTo(false)
            assertThat(connection.isConnected()).isEqualTo(true)
            val queryResultFuture = connection.sendQuery("select sleep(1)")
            assertThat((queryResultFuture.get(10, TimeUnit.SECONDS)).rows.size).isEqualTo(1)
            assertThat(connection.isTimeout()).isEqualTo(false)
            assertThat(connection.isConnected()).isEqualTo(true)
            pool.giveBack(connection).get(10, TimeUnit.SECONDS)
            await.untilCallTo { pool.idleConnectionsCount } matches { it == 1 } // connection returned to pool
        }
    }

    @Test
    fun `inTransaction with short timeout surfaces TimeoutException and allows pool recovery`() {
        val txTimeoutConfig = ContainerHelper.defaultConfiguration.copy(queryTimeout = Duration.ofMillis(150))
        withConfigurablePool(txTimeoutConfig) { pool ->
            verifyException(ExecutionException::class.java, TimeoutException::class.java) {
                pool.inTransaction { conn ->
                    conn.sendQuery("select 1").flatMapAsync(ExecutorServiceUtils.CommonPool) {
                        conn.sendQuery("select sleep(10)")
                    }
                }.get(10, TimeUnit.SECONDS)
            }

            assertThatThrownBy {
                runBlocking {
                    pool.asSuspending.inTransaction { tx ->
                        tx.sendQuery("select 1")
                        tx.sendQuery("select sleep(10)")
                    }
                }
            }.isInstanceOf(TimeoutException::class.java)

            await.untilCallTo { pool.inUseConnectionsCount } matches { it == 0 }
            val rows = pool.sendQuery("select 1").get(10, TimeUnit.SECONDS).rows
            assertThat(rows.size).isEqualTo(1)
        }
    }

    private fun shortTimeoutConfiguration() =
        ContainerHelper.defaultConfiguration.copy(queryTimeout = (Duration.ofMillis(10)))

    private fun longTimeoutConfiguration() =
        ContainerHelper.defaultConfiguration.copy(queryTimeout = (Duration.ofSeconds(5)))
}
