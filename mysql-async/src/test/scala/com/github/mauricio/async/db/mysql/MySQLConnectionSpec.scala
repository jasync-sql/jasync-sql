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

package com.github.mauricio.async.db.mysql

import org.specs2.mutable.Specification
import com.github.mauricio.async.db.Configuration
import com.github.mauricio.async.db.util.FutureUtils.await

class MySQLConnectionSpec extends Specification {

  val configuration = new Configuration(
    "mysql_async",
    "localhost",
    port = 3306,
    password = Some("root"),
    database = Some("mysql_async_tests")
  )

  val rootConfiguration = new Configuration(
    "root",
    "localhost",
    port = 3306,
    password = None,
    database = Some("mysql_async_tests")
  )

  "connection" should {

    "connect to a MySQL instance with a password" in {
      val connection = new MySQLConnection(configuration)
      await(connection.connect) === connection
    }

    "connect to a MySQL instance without password" in {
      val connection = new MySQLConnection(rootConfiguration)
      await(connection.connect) === connection
    }

  }

  def withConnection[T]( fn : (MySQLConnection) => T )( cfg : Configuration = configuration ) : T = {

    val connection = new MySQLConnection(cfg)
    try {
      fn(connection)
    } finally {
      await( connection.close )
    }


  }

}