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

import com.github.mauricio.async.db.postgresql.{PostgreSQLConnection, DatabaseTestHelper}
import org.specs2.mutable.Specification
import com.github.mauricio.async.db.pool.{ConnectionPool, PoolConfiguration}

class ConnectionPoolSpec extends Specification with DatabaseTestHelper {

  "pool" should {

    "give you a connection when sending statements" in {

      withPool{
        pool =>
          executeQuery(pool, "SELECT 8").rows.get(0)(0) === 8
          pool.availables.size === 1
      }

    }

    "give you a connection for prepared statements" in {
      withPool{
        pool =>
          executePreparedStatement(pool, "SELECT 8").rows.get(0)(0) === 8
          pool.availables.size === 1
      }
    }

    "return an empty map when connect is called" in {
      withPool {
        pool =>
          await(pool.connect) === Map[String,String]()
      }
    }

  }

  def withPool[R]( fn : (ConnectionPool[PostgreSQLConnection]) => R ) : R = {

    val pool = new ConnectionPool( new ConnectionObjectFactory(defaultConfiguration), PoolConfiguration.Default )
    try {
      fn(pool)
    } finally {
      await(pool.disconnect)
    }

  }

}
