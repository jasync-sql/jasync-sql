package com.github.aysnc.sql.db.integration.pool

import com.github.aysnc.sql.db.integration.DatabaseTestHelper
import com.github.aysnc.sql.db.verifyException
import com.github.jasync.sql.db.exceptions.ConnectionNotConnectedException
import com.github.jasync.sql.db.exceptions.ConnectionStillRunningQueryException
import com.github.jasync.sql.db.invoke
import com.github.jasync.sql.db.pool.ActorBasedObjectPool
import com.github.jasync.sql.db.pool.AsyncObjectPool
import com.github.jasync.sql.db.pool.PoolConfiguration
import com.github.jasync.sql.db.pool.PoolExhaustedException
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import com.github.jasync.sql.db.postgresql.pool.PostgreSQLConnectionFactory
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class ActorAsyncObjectPoolSpec : DatabaseTestHelper() {

    @Test
    fun `"pool" should "give me a valid object when I ask for one"`() {

        withPool { pool ->
            val connection = get(pool)
            val result = executeTest(connection)
            pool.giveBack(connection)
            result
        }
    }

    @Test
    fun `"pool" should "enqueue an action if the pool is full" `() {

        withPool(1, 3) { pool ->

            val connection = get(pool)
            val promises: List<CompletableFuture<PostgreSQLConnection>> = listOf(pool.take(), pool.take(), pool.take())

            assertThat(pool.availableItems.size).isEqualTo(0)
            assertThat(pool.usedItems.size).isEqualTo(1)
            assertThat(pool.waitingForItem.size).isLessThanOrEqualTo(3)

            /* pool.take call checkout that call this.mainPool.action,
            so enqueuePromise called in executorService,
            so there is no guaranties that all promises in queue at that moment
             */
            val deadline = TimeUnit.SECONDS.toMillis(5)
            while (pool.waitingForItem.size < 3 || System.currentTimeMillis() < deadline) {
                Thread.sleep(50)
            }

            assertThat(pool.waitingForItem.size).isEqualTo(3)

            executeTest(connection)

            pool.giveBack(connection).get()

            val pools: List<CompletableFuture<AsyncObjectPool<PostgreSQLConnection>>> = promises.map { promise ->
                val con = promise.get(5, TimeUnit.SECONDS)
                executeTest(con)
                pool.giveBack(con)
            }

            pools.map { it.get(5, TimeUnit.SECONDS) }

            await.untilCallTo { pool.availableItems.size } matches { it == 1 }
            assertThat(pool.usedItems.size).isEqualTo(0)
            assertThat(pool.waitingForItem.size).isEqualTo(0)

        }

    }

    @Test
    fun `"pool" should "exhaust the pool"`() {

        withPool(1, 1) { pool ->
            (1..2).forEach { _ ->
                pool.take()
            }
            verifyException(ExecutionException::class.java, PoolExhaustedException::class.java) {
                awaitFuture(pool.take())
            }
        }

    }

    @Test
    fun `"pool" should "it should remove idle connections once the time limit has been reached" `() {

        withPool(validationInterval = 1000) { pool ->
            val connections = (1..5).map { _ ->
                val connection = get(pool)
                executeTest(connection)
                connection
            }

            connections.forEach { connection -> awaitFuture(pool.giveBack(connection)) }

            await.untilCallTo { pool.availableItems.size } matches { it == 5 }

            Thread.sleep(2000)

            await.untilCallTo { pool.availableItems } matches { it.isNullOrEmpty() }
        }

    }

    @Test
    fun `"pool" should "it should remove aged out connections once the time limit has been reached" `() {

        withPool(validationInterval = 1000, maxTtl = 1000) { pool ->
            val connections = (1..5).map { _ ->
                val connection = get(pool)
                executeTest(connection)
                connection
            }

            Thread.sleep(2000)
            connections.forEach { connection -> awaitFuture(pool.giveBack(connection)) }

            await.untilCallTo { pool.availableItems.size } matches { it == 5 }


            await.untilCallTo { pool.availableItems } matches { it.isNullOrEmpty() }
        }

    }

    @Test
    fun `"pool" should "it should validate returned connections before sending them back to the pool" `() {

        withPool { pool ->
            val connection = get(pool)
            awaitFuture(connection.disconnect())

            assertThat(pool.usedItems.size).isEqualTo(1)

            verifyException(ExecutionException::class.java, ConnectionNotConnectedException::class.java) {
                awaitFuture(pool.giveBack(connection))
            }

            assertThat(pool.availableItems.size).isEqualTo(0)
            assertThat(pool.usedItems.size).isEqualTo(0)
        }

    }

    @Test
    fun `"pool" should "it should not accept returned connections that aren't ready for query" `() {

        withPool { pool ->
            val connection = get(pool)
            connection.sendPreparedStatement("SELECT pg_sleep(3)")

            verifyException(ExecutionException::class.java, ConnectionStillRunningQueryException::class.java) {
                awaitFuture(pool.giveBack(connection))
            }
            await.untilCallTo { pool.availableItems.size } matches { it == 0 }
            await.untilCallTo { pool.usedItems.size } matches { it == 0 }
        }

    }


    private fun <T> withPool(
        maxObjects: Int = 5,
        maxQueueSize: Int = 5,
        validationInterval: Long = 3000,
        //maxIdle: Long = 1000,
        maxTtl: Long = -1,
        fn: (ActorBasedObjectPool<PostgreSQLConnection>) -> T
    ): T {

        val poolConfiguration = PoolConfiguration(
            maxIdle = maxTtl,
            maxObjects = maxObjects,
            maxQueueSize = maxQueueSize,
            validationInterval = validationInterval
        )
        val factory = PostgreSQLConnectionFactory(this.conf)
        val pool = ActorBasedObjectPool<PostgreSQLConnection>(factory, poolConfiguration)

        try {
            return fn(pool)
        } finally {
            pool.close().get()
        }

    }

    private fun executeTest(connection: PostgreSQLConnection) =
        assertThat(executeQuery(connection, "SELECT 0").rows.get(0)(0)).isEqualTo(0)

    fun get(pool: ActorBasedObjectPool<PostgreSQLConnection>): PostgreSQLConnection {
        val future = pool.take()
        return future.get(5, TimeUnit.SECONDS)
    }

}
