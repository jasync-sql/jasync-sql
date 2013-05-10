/*
 * Copyright 2013 Maurício Linhares
 *
 * Maurício Linhares licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.github.mauricio.async.db.postgresql.pool

import com.github.mauricio.async.db.pool.{SingleThreadedAsyncObjectPool, PoolExhaustedException, PoolConfiguration}
import com.github.mauricio.async.db.postgresql.{DatabaseTestHelper, PostgreSQLConnection}
import java.nio.channels.ClosedChannelException
import java.util.concurrent.TimeUnit
import org.specs2.mutable.Specification
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import com.github.mauricio.async.db.postgresql.exceptions.ConnectionStillRunningQueryException

class SingleThreadedAsyncObjectPoolSpec extends Specification with DatabaseTestHelper {

  "pool" should {

    "give me a valid object when I ask for one" in {

      withPool {
        pool =>
          val connection = get(pool)
          val result = executeTest(connection)
          pool.giveBack(connection)
          result
      }
    }

    "enqueue an action if the pool is full" in {

      withPool({
        pool =>

          val connection = get(pool)
          val promises = List(pool.take, pool.take, pool.take)

          pool.availables.size === 0
          pool.inUse.size === 1
          pool.queued.size === 3

          executeTest(connection)

          pool.giveBack(connection)

          promises.foreach {
            promise =>
              val connection = Await.result(promise, Duration(5, TimeUnit.SECONDS))
              executeTest(connection)
              pool.giveBack(connection)
          }

          pool.availables.size === 1
          pool.inUse.size === 0
          pool.queued.size === 0

      }, 1, 3)

    }

    "exhaust the pool" in {

      withPool({
        pool =>
          1 to 2 foreach {
            _ => pool.take
          }
          await(pool.take) must throwA[PoolExhaustedException]
      }, 1, 1)

    }

    "it shoudl remove idle connections once the time limit has been reached" in {

      withPool({
        pool =>
          val connections = 1 to 5 map {
            _ =>
              val connection = get(pool)
              executeTest(connection)
              connection
          }

          connections.foreach(connection => await(pool.giveBack(connection)))

          pool.availables.size === 5

          Thread.sleep(2000)

          pool.availables.isEmpty must beTrue

      }, validationInterval = 1000)

    }

    "it should validate returned connections before sending them back to the pool" in {

      withPool {
        pool =>
          val connection = get(pool)
          await(connection.disconnect)

          pool.inUse.size === 1

          await(pool.giveBack(connection)) must throwA[ClosedChannelException]

          pool.availables.size === 0
          pool.inUse.size === 0
      }

    }
    
    "it should not accept returned connections that aren't ready for query" in {

      withPool {
        pool =>
          val connection = get(pool)
          connection.sendPreparedStatement("select 1")

          await(pool.giveBack(connection)) must throwA[ConnectionStillRunningQueryException]
          pool.availables.size === 0
          pool.inUse.size === 0
      }

    }

  }

  def withPool[T](
                   fn: (SingleThreadedAsyncObjectPool[PostgreSQLConnection]) => T,
                   maxObjects: Int = 5,
                   maxQueueSize: Int = 5,
                   validationInterval: Long = 3000
                   ): T = {

    val poolConfiguration = new PoolConfiguration(
      maxIdle = 1000,
      maxObjects = maxObjects,
      maxQueueSize = maxQueueSize,
      validationInterval = validationInterval
    )
    val factory = new ConnectionObjectFactory(this.defaultConfiguration)
    val pool = new SingleThreadedAsyncObjectPool[PostgreSQLConnection](factory, poolConfiguration)

    try {
      fn(pool)
    } finally {
      await(pool.close)
    }

  }

  def executeTest(connection: PostgreSQLConnection) = executeQuery(connection, "SELECT 0").rows.get(0)(0) === 0

  def get(pool: SingleThreadedAsyncObjectPool[PostgreSQLConnection]): PostgreSQLConnection = {
    val future = pool.take
    Await.result(future, Duration(5, TimeUnit.SECONDS))
  }

}
