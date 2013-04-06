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

package com.github.mauricio.postgresql.pool

import com.github.mauricio.async.db.Configuration
import com.github.mauricio.postgresql.DatabaseConnectionHandler
import concurrent.Await
import concurrent.duration._
import org.apache.commons.pool.PoolableObjectFactory

class ConnectionObjectFactory(
                              configuration : Configuration)
  extends PoolableObjectFactory[DatabaseConnectionHandler] {

  def makeObject(): DatabaseConnectionHandler = {
    val connection = new DatabaseConnectionHandler(configuration)
    Await.result( connection.connect, 5 seconds )
    connection
  }

  def destroyObject(obj: DatabaseConnectionHandler) {
    obj.disconnect
  }

  def validateObject(obj: DatabaseConnectionHandler): Boolean = {
    obj.isConnected
  }

  def activateObject(obj: DatabaseConnectionHandler) {
    //no op
  }

  def passivateObject(obj: DatabaseConnectionHandler) {
    //no op
  }
}
