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

import com.github.mauricio.async.db.Configuration
import com.github.mauricio.async.db.exceptions.ConnectionTimeoutedException
import com.github.mauricio.async.db.pool.ObjectFactory
import com.github.mauricio.async.db.postgresql.PostgreSQLConnection
import com.github.mauricio.async.db.util.Log
import java.nio.channels.ClosedChannelException
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Success, Failure, Try}
import scala.concurrent.ExecutionContext
import com.github.mauricio.async.db.util.ExecutorServiceUtils
import com.github.mauricio.async.db.util.NettyUtils
import io.netty.channel.EventLoopGroup

object PostgreSQLConnectionFactory {
  val log = Log.get[PostgreSQLConnectionFactory]
}

/**
 *
 * Object responsible for creating new connection instances.
 *
 * @param configuration
 */

class PostgreSQLConnectionFactory( 
    val configuration : Configuration, 
    group : EventLoopGroup = NettyUtils.DefaultEventLoopGroup,
    executionContext : ExecutionContext = ExecutorServiceUtils.CachedExecutionContext ) extends ObjectFactory[PostgreSQLConnection] {

  import PostgreSQLConnectionFactory.log

  def create: PostgreSQLConnection = {
    val connection = new PostgreSQLConnection(configuration, group = group, executionContext = executionContext)
    Await.result(connection.connect, configuration.connectTimeout)

    connection
  }

  def destroy(item: PostgreSQLConnection) {
    item.disconnect
  }

  /**
   *
   * Validates by checking if the connection is still connected to the database or not.
   *
   * @param item an object produced by this pool
   * @return
   */

  def validate( item : PostgreSQLConnection ) : Try[PostgreSQLConnection] = {
    Try {
      if ( item.isTimeouted ) {
        throw new ConnectionTimeoutedException(item)
      }
      if ( !item.isConnected || item.hasRecentError ) {
        throw new ClosedChannelException()
      } 
      item.validateIfItIsReadyForQuery("Trying to give back a connection that is not ready for query")
      item
    }
  }

  /**
   *
   * Tests whether we can still send a **SELECT 0** statement to the database.
   *
   * @param item an object produced by this pool
   * @return
   */

  override def test(item: PostgreSQLConnection): Try[PostgreSQLConnection] = {
    val result : Try[PostgreSQLConnection] = Try({
      Await.result( item.sendQuery("SELECT 0"), configuration.testTimeout )
      item
    })

    result match {
      case Failure(e) => {
        try {
          if ( item.isConnected ) {
            item.disconnect
          }
        } catch {
          case e : Exception => log.error("Failed disconnecting object", e)
        }
        result
      }
      case Success(i) => {
        result
      }
    }
  }

}
