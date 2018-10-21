package com.github.aysnc.sql.db.integration.pool

import com.github.aysnc.sql.db.integration.DatabaseTestHelper
import com.github.aysnc.sql.db.verifyException
import com.github.jasync.sql.db.exceptions.ConnectionStillRunningQueryException
import com.github.jasync.sql.db.invoke
import com.github.jasync.sql.db.pool.AsyncObjectPool
import com.github.jasync.sql.db.pool.PoolConfiguration
import com.github.jasync.sql.db.pool.PoolExhaustedException
import com.github.jasync.sql.db.pool.SingleThreadedAsyncObjectPool
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import com.github.jasync.sql.db.postgresql.pool.PostgreSQLConnectionFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.nio.channels.ClosedChannelException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class SingleThreadedAsyncObjectPoolSpec : DatabaseTestHelper() {

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

    withPool ( 1, 3) { pool ->

      val connection = get(pool)
      val promises: List<CompletableFuture<PostgreSQLConnection>> = listOf(pool.take(), pool.take(), pool.take())

      assertThat(pool.availables().size).isEqualTo(0)
      assertThat(pool.inUse().size).isEqualTo(1)
      assertThat(pool.queued().size).isLessThanOrEqualTo(3)

      /* pool.take call checkout that call this.mainPool.action,
      so enqueuePromise called in executorService,
      so there is no guaranties that all promises in queue at that moment
       */
      val deadline = TimeUnit.SECONDS.toMillis(5)
      while (pool.queued().size < 3 || System.currentTimeMillis() < deadline) {
        Thread.sleep(50)
      }

      assertThat(pool.queued().size).isEqualTo(3)

      executeTest(connection)

      pool.giveBack(connection)

      val pools: List<CompletableFuture<AsyncObjectPool<PostgreSQLConnection>>> = promises.map { promise ->
        val con = promise.get(5, TimeUnit.SECONDS)
        executeTest(con)
        pool.giveBack(con)
      }

      pools.last().get(5, TimeUnit.SECONDS)

      assertThat(pool.availables().size).isEqualTo(1)
      assertThat(pool.inUse().size).isEqualTo(0)
      assertThat(pool.queued().size).isEqualTo(0)

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

    withPool (validationInterval = 1000) { pool ->
      val connections = (1..5).map { _ ->
        val connection = get(pool)
        executeTest(connection)
        connection
      }

      connections.forEach { connection -> awaitFuture(pool.giveBack(connection)) }

      assertThat(pool.availables().size).isEqualTo(5)

      Thread.sleep(2000)

      assertThat(pool.availables()).isEmpty()

    }

  }

  @Test
  fun `"pool" should "it should validate returned connections before sending them back to the pool" `() {

    withPool { pool ->
      val connection = get(pool)
      awaitFuture(connection.disconnect())

      assertThat(pool.inUse().size).isEqualTo(1)

      verifyException(ExecutionException::class.java, ClosedChannelException::class.java) {
        awaitFuture(pool.giveBack(connection))
      }

      assertThat(pool.availables().size).isEqualTo(0)
      assertThat(pool.inUse().size).isEqualTo(0)
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
      assertThat(pool.availables().size).isEqualTo(0)
      assertThat(pool.inUse().size).isEqualTo(0)
    }

  }


  private fun <T> withPool(
      maxObjects: Int = 5,
      maxQueueSize: Int = 5,
      validationInterval: Long = 3000,
      fn: (SingleThreadedAsyncObjectPool<PostgreSQLConnection>) -> T
  ): T {

    val poolConfiguration = PoolConfiguration(
        maxIdle = 1000,
        maxObjects = maxObjects,
        maxQueueSize = maxQueueSize,
        validationInterval = validationInterval
    )
    val factory = PostgreSQLConnectionFactory(this.conf)
    val pool = SingleThreadedAsyncObjectPool<PostgreSQLConnection>(factory, poolConfiguration)

    try {
      return fn(pool)
    } finally {
      pool.close().get()
    }

  }

  private fun executeTest(connection: PostgreSQLConnection) = assertThat(executeQuery(connection, "SELECT 0").rows!!.get(0)(0)).isEqualTo(0)

  fun get(pool: SingleThreadedAsyncObjectPool<PostgreSQLConnection>): PostgreSQLConnection {
    val future = pool.take()
    return future.get(5, TimeUnit.SECONDS)
  }

}
