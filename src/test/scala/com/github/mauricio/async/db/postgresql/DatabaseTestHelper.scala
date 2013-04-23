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

import com.github.mauricio.async.db.Configuration
import concurrent.Await
import concurrent.duration._

trait DatabaseTestHelper {

  def databaseName = Some("netty_driver_test")

  def databasePort = 5433

  def defaultConfiguration = new Configuration(
    port = databasePort,
    username = "postgres",
    database = databaseName)

  def withHandler[T](fn: (DatabaseConnectionHandler) => T): T = {
    withHandler(this.defaultConfiguration, fn)
  }

  def withHandler[T](configuration: Configuration, fn: (DatabaseConnectionHandler) => T): T = {

    val handler = new DatabaseConnectionHandler(configuration)

    try {
      Await.result(handler.connect, Duration(5, SECONDS))
      fn(handler)
    } finally {
      handler.disconnect
    }

  }

  def executeDdl(handler: DatabaseConnectionHandler, data: String, count: Int = 0) = {
    val rows = Await.result(handler.sendQuery(data), Duration(5, SECONDS)).rowsAffected

    if (rows != count) {
      throw new IllegalStateException("We expected %s rows but there were %s".format(count, rows))
    }

  }

  def executeQuery(handler: DatabaseConnectionHandler, data: String) = {
    Await.result(handler.sendQuery(data), Duration(5, SECONDS))
  }

  def executePreparedStatement(
                                handler: DatabaseConnectionHandler,
                                statement: String,
                                values: Array[Any] = Array.empty[Any]) = {
    Await.result(handler.sendPreparedStatement(statement, values), Duration(5, SECONDS))
  }

}
