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

import com.github.mauricio.async.db.util.FutureUtils.await
import com.github.mauricio.async.db.{QueryResult, Configuration}

trait ConnectionHelper {

  def defaultConfiguration = new Configuration(
    "mysql_async",
    "localhost",
    port = 3306,
    password = Some("root"),
    database = Some("mysql_async_tests")
  )

  def withConnection[T]( fn : (MySQLConnection) => T ) : T = {

    val connection = new MySQLConnection(this.defaultConfiguration)

    try {
      await( connection.connect )
      fn(connection)
    } finally {
      await( connection.close )
    }

  }

  def executeQuery( connection : MySQLConnection, query : String  ) : QueryResult = {
    await( connection.sendQuery(query) )
  }

}
