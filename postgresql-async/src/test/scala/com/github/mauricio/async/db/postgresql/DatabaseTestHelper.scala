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

package com.github.mauricio.async.db.postgresql

import com.github.mauricio.async.db.util.Log
import com.github.mauricio.async.db.{Connection, Configuration}
import java.util.concurrent.{TimeoutException, TimeUnit}
import scala.Some
import scala.concurrent.duration._
import scala.concurrent.{Future, Await}

object DatabaseTestHelper {
  val log = Log.get[DatabaseTestHelper]
}

trait DatabaseTestHelper {


  def databaseName = Some("netty_driver_test")

  def databasePort = 5432

  def defaultConfiguration = new Configuration(
    port = databasePort,
    username = "postgres",
    database = databaseName)

  def withHandler[T](fn: (PostgreSQLConnection) => T): T = {
    withHandler(this.defaultConfiguration, fn)
  }

  def withHandler[T](configuration: Configuration, fn: (PostgreSQLConnection) => T): T = {

    val handler = new PostgreSQLConnection(configuration)

    try {
      Await.result(handler.connect, Duration(5, SECONDS))
      fn(handler)
    } finally {
      handleTimeout(handler, handler.disconnect)
    }

  }

  def executeDdl(handler: Connection, data: String, count: Int = 0) = {
    val rows = handleTimeout(handler, {
      Await.result(handler.sendQuery(data), Duration(5, SECONDS)).rowsAffected
    })

    if (rows != count) {
      throw new IllegalStateException("We expected %s rows but there were %s".format(count, rows))
    }
  }

  private def handleTimeout[R]( handler : Connection, fn : => R ) = {
    try {
      fn
    } catch {
      case e : TimeoutException => {
        throw new IllegalStateException("Timeout executing call from handler -> %s".format( handler))
      }
    }
  }

  def executeQuery(handler: Connection, data: String) = {
    handleTimeout( handler, {
      Await.result(handler.sendQuery(data), Duration(5, SECONDS))
    } )
  }

  def executePreparedStatement(
                                handler: Connection,
                                statement: String,
                                values: Array[Any] = Array.empty[Any]) = {
    handleTimeout( handler, {
      Await.result(handler.sendPreparedStatement(statement, values), Duration(5, SECONDS))
    } )
  }

  def await[T](future: Future[T]): T = {
    Await.result(future, Duration(5, TimeUnit.SECONDS))
  }


}
