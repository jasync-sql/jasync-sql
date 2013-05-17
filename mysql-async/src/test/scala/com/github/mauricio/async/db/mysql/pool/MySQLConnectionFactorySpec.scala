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

package com.github.mauricio.async.db.mysql.pool

import com.github.mauricio.async.db.mysql.ConnectionHelper
import com.github.mauricio.async.db.util.FutureUtils.await
import org.specs2.mutable.Specification
import scala.util.{Try, Failure}

class MySQLConnectionFactorySpec extends Specification with ConnectionHelper {

  val factory = new MySQLConnectionFactory(this.defaultConfiguration)

  "factory" should {

    "fail validation if a connection has errored" in {

      val connection = factory.create
      val result = Try {
        executeQuery(connection, "this is not sql")
      }

      factory.validate(connection) match {
        case Failure(e) => ok("connection sucessfully rejected")
      }

    }

    "be able to provide connections to the pool" in {
      withPool {
        pool =>
          executeQuery(pool, "SELECT 0").rows.get(0)(0) === 0
      }
    }

    "fail validation if a connection is disconnected" in {

      val connection = factory.create

      await(connection.disconnect)

      factory.validate(connection) match {
        case Failure(e) => ok("Connection successfully rejected")
      }

    }

    "fail validation if a connection is still waiting for a query" in {
      val connection = factory.create
      connection.sendQuery("SELECT SLEEP(5)")

      Thread.sleep(1000)

      factory.validate(connection) match {
        case Failure(e) => ok("connection successfully rejected")
      }
    }

    "accept a good connection" in {
      val connection = factory.create

      factory.validate(connection) match {
        case scala.util.Success(c) => ok("connection successfully accepted")
      }
    }

    "test a valid connection and say it is ok" in {

      val connection = factory.create

      factory.test(connection) match {
        case scala.util.Success(c) => ok("connection successfully accepted")
      }

    }

    "fail test if a connection is disconnected" in {

      val connection = factory.create

      await(connection.disconnect)

      factory.test(connection) match {
        case Failure(e) => ok("Connection successfully rejected")
      }

    }

  }

}
