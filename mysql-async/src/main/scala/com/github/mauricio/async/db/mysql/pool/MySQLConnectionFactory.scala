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

import com.github.mauricio.async.db.Configuration
import com.github.mauricio.async.db.pool.ObjectFactory
import com.github.mauricio.async.db.mysql.MySQLConnection
import scala.util.Try
import scala.concurrent.Await
import scala.concurrent.duration._
import com.github.mauricio.async.db.util.Log
import com.github.mauricio.async.db.exceptions.{ConnectionStillRunningQueryException, ConnectionNotConnectedException}

object MySQLConnectionFactory {
  final val log = Log.get[MySQLConnectionFactory]
}

/**
 *
 * Connection pool factory for [[com.github.mauricio.async.db.mysql.MySQLConnection]] objects.
 *
 * @param configuration a valid configuration to connect to a MySQL server.
 *
 */

class MySQLConnectionFactory( configuration : Configuration ) extends ObjectFactory[MySQLConnection] {

  import MySQLConnectionFactory.log

  /**
   *
   * Creates a valid object to be used in the pool. This method can block if necessary to make sure a correctly built
   * is created.
   *
   * @return
   */
  def create: MySQLConnection = {
    val connection = new MySQLConnection(configuration)
    Await.result(connection.connect, configuration.connectTimeout )

    connection
  }

  /**
   *
   * This method should "close" and release all resources acquired by the pooled object. This object will not be used
   * anymore so any cleanup necessary to remove it from memory should be made in this method. Implementors should not
   * raise an exception under any circumstances, the factory should log and clean up the exception itself.
   *
   * @param item
   */
  def destroy(item: MySQLConnection) {
    try {
      item.disconnect
    } catch {
      case e : Exception => {
        log.error("Failed to close the connection", e)
      }
    }
  }

  /**
   *
   * Validates that an object can still be used for it's purpose. This method should test the object to make sure
   * it's still valid for clients to use. If you have a database connection, test if you are still connected, if you're
   * accessing a file system, make sure you can still see and change the file.
   *
   * You decide how fast this method should return and what it will test, you should usually do something that's fast
   * enough not to slow down the pool usage, since this call will be made whenever an object returns to the pool.
   *
   * If this object is not valid anymore, a [[scala.util.Failure]] should be returned, otherwise [[scala.util.Success]]
   * should be the result of this call.
   *
   * @param item an object produced by this pool
   * @return
   */
  def validate(item: MySQLConnection): Try[MySQLConnection] = {
    Try{

      if ( !item.isConnected ) {
        throw new ConnectionNotConnectedException(item)
      }

      if (item.lastException != null) {
        throw item.lastException
      }

      if ( item.isQuerying ) {
        throw new ConnectionStillRunningQueryException(item.count, false)
      }

      item
    }
  }

  /**
   *
   * Does a full test on the given object making sure it's still valid. Different than validate, that's called whenever
   * an object is given back to the pool and should usually be fast, this method will be called when objects are
   * idle to make sure they don't "timeout" or become stale in anyway.
   *
   * For convenience, this method defaults to call **validate** but you can implement it in a different way if you
   * would like to.
   *
   * @param item an object produced by this pool
   * @return
   */
  override def test(item: MySQLConnection): Try[MySQLConnection] = {
    Try {
      Await.result(item.sendQuery("SELECT 0"), configuration.testTimeout)
      item
    }
  }
}
