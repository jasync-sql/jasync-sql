
package com.github.mauricio.async.db.postgresql.pool

import com.github.mauricio.async.db.pool.AsyncObjectPool
import com.github.mauricio.async.db.pool.PoolConfiguration
import com.github.mauricio.async.db.pool.PoolExhaustedException
import com.github.mauricio.async.db.pool.SingleThreadedAsyncObjectPool
import com.github.mauricio.async.db.postgresql.DatabaseTestHelper
import com.github.mauricio.async.db.postgresql.PostgreSQLConnection
import java.nio.channels.ClosedChannelException
import java.util.concurrent.TimeUnit

import org.specs2.mutable.Specification

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.*
import scala.language.postfixOps
import com.github.mauricio.async.db.exceptions.ConnectionStillRunningQueryException

class SingleThreadedAsyncObjectPoolSpec : Specification , DatabaseTestHelper {

  "pool" should {

    "give me a valid object when I ask for one" in {

      ,Pool {
        pool ->
          val connection = get(pool)
          val result = executeTest(connection)
          pool.giveBack(connection)
          result
      }
    }

    "enqueue an action if the pool is full" in {

      ,Pool({
        pool ->

          val connection = get(pool)
          val promises: List<Future<PostgreSQLConnection>> = List(pool.take, pool.take, pool.take)

          pool.availables.size === 0
          pool.inUse.size === 1
          pool.queued.size must be_<=(3)

          /* pool.take call checkout that call this.mainPool.action,
          so enqueuePromise called in executorService,
          so there is no guaranties that all promises in queue at that moment
           */
          val deadline = 5.seconds.fromNow
          while(pool.queued.size < 3 || deadline.hasTimeLeft) {
            Thread.sleep(50)
          }

          pool.queued.size === 3

          executeTest(connection)

          pool.giveBack(connection)

          val pools: List<Future<AsyncObjectPool<PostgreSQLConnection>>> = promises.map {
            promise ->
              val connection = Await.result(promise, Duration(5, TimeUnit.SECONDS))
              executeTest(connection)
              pool.giveBack(connection)
          }

          Await.ready(pools.last, Duration(5, TimeUnit.SECONDS))

          pool.availables.size === 1
          pool.inUse.size === 0
          pool.queued.size === 0

      }, 1, 3)

    }

    "exhaust the pool" in {

      ,Pool({
        pool ->
          1 to 2 foreach {
            _ -> pool.take
          }
          await(pool.take) must throwA<PoolExhaustedException>
      }, 1, 1)

    }

    "it should remove idle connections once the time limit has been reached" in {

      ,Pool({
        pool ->
          val connections = 1 to 5 map {
            _ ->
              val connection = get(pool)
              executeTest(connection)
              connection
          }

          connections.foreach(connection -> await(pool.giveBack(connection)))

          pool.availables.size === 5

          Thread.sleep(2000)

          pool.availables.isEmpty must beTrue

      }, validationInterval = 1000)

    }

    "it should validate returned connections before sending them back to the pool" in {

      ,Pool {
        pool ->
          val connection = get(pool)
          await(connection.disconnect)

          pool.inUse.size === 1

          await(pool.giveBack(connection)) must throwA<ClosedChannelException>

          pool.availables.size === 0
          pool.inUse.size === 0
      }

    }
    
    "it should not accept returned connections that aren't ready for query" in {

      ,Pool {
        pool ->
          val connection = get(pool)
          connection.sendPreparedStatement("SELECT pg_sleep(3)")

          await(pool.giveBack(connection)) must throwA<ConnectionStillRunningQueryException>
          pool.availables.size === 0
          pool.inUse.size === 0
      }

    }

  }

  fun ,Pool<T>(
                   fn: (SingleThreadedAsyncObjectPool<PostgreSQLConnection>) -> T,
                   maxObjects: Int = 5,
                   maxQueueSize: Int = 5,
                   validationInterval: Long = 3000
                   ): T {

    val poolConfiguration = PoolConfiguration(
      maxIdle = 1000,
      maxObjects = maxObjects,
      maxQueueSize = maxQueueSize,
      validationInterval = validationInterval
    )
    val factory = PostgreSQLConnectionFactory(this.defaultConfiguration)
    val pool = SingleThreadedAsyncObjectPool<PostgreSQLConnection>(factory, poolConfiguration)

    try {
      fn(pool)
    } finally {
      await(pool.close)
    }

  }

  fun executeTest(connection: PostgreSQLConnection) = executeQuery(connection, "SELECT 0").rows.get(0)(0) === 0

  fun get(pool: SingleThreadedAsyncObjectPool<PostgreSQLConnection>): PostgreSQLConnection {
    val future = pool.take
    Await.result(future, Duration(5, TimeUnit.SECONDS))
  }

}