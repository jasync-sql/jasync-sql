
package com.github.mauricio.async.db.postgresql.pool

import java.util.UUID

import com.github.mauricio.async.db.pool.ConnectionPool
import com.github.mauricio.async.db.pool.PoolConfiguration
import com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException
import com.github.mauricio.async.db.postgresql.PostgreSQLConnection
import com.github.mauricio.async.db.postgresql.DatabaseTestHelper
import org.specs2.mutable.Specification
import scala.concurrent.ExecutionContext.Implicits.global

object ConnectionPoolSpec {
  val Insert = "insert into transaction_test (id) values (?)"
}

class ConnectionPoolSpec : Specification , DatabaseTestHelper {

  import ConnectionPoolSpec.Insert

  "pool" should {

    "give you a connection when sending statements" in {

      ,Pool{
        pool ->
          executeQuery(pool, "SELECT 8").rows.get(0)(0) === 8
          Thread.sleep(1000)
          pool.availables.size === 1
      }

    }

    "give you a connection for prepared statements" in {
      ,Pool{
        pool ->
          executePreparedStatement(pool, "SELECT 8").rows.get(0)(0) === 8
          Thread.sleep(1000)
          pool.availables.size === 1
      }
    }

    "return an empty map when connect is called" in {
      ,Pool {
        pool ->
          await(pool.connect) === pool
      }
    }

    "runs commands for a transaction in a single connection" in {

      val id = UUID.randomUUID().toString

      ,Pool {
        pool ->
          val operations = pool.inTransaction {
            connection ->
              connection.sendPreparedStatement(Insert, List(id)).flatMap {
                result ->
                  connection.sendPreparedStatement(Insert, List(id)).map {
                    failure ->
                      List(result, failure)
                  }
              }
          }

          await(operations) must throwA<GenericDatabaseException>

      }

    }

  }

  fun ,Pool<R>( fn : (ConnectionPool<PostgreSQLConnection>) -> R ) : R {

    val pool = ConnectionPool( PostgreSQLConnectionFactory(defaultConfiguration), PoolConfiguration.Default )
    try {
      fn(pool)
    } finally {
      await(pool.disconnect)
    }

  }

}