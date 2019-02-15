package com.github.jasync.sql.db.mysql

import com.github.jasync.sql.db.exceptions.ConnectionTimeoutedException
import org.assertj.core.api.Assertions.assertThat
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
    fun `"Simple query with short timeout"`() {
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
    fun `"Simple query with short timeout directly on pool"`() {
        withConfigurablePool(shortTimeoutConfiguration()) { pool ->
            val queryResultFuture = pool.sendQuery("select sleep(100)")
            verifyException(ExecutionException::class.java, TimeoutException::class.java) {
                queryResultFuture.get(10, TimeUnit.SECONDS)
            }
            await.untilCallTo { pool.idleConnectionsCount } matches { it == 0 } // connection removed from pool
        }
    }

    @Test
    fun `"Simple query with 5 sec timeout"`() {
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

    private fun shortTimeoutConfiguration() =
        ContainerHelper.defaultConfiguration.copy(queryTimeout = (Duration.ofMillis(10)))

    private fun longTimeoutConfiguration() =
        ContainerHelper.defaultConfiguration.copy(queryTimeout = (Duration.ofSeconds(5)))

}
