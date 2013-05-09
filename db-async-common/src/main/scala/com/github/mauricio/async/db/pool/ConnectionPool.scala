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

package com.github.mauricio.async.db.pool

import com.github.mauricio.async.db.util.ExecutorServiceUtils
import com.github.mauricio.async.db.{QueryResult, Connection}
import scala.concurrent.{ExecutionContext, Future}

/**
 *
 * Pool specialized in database connections that also simplifies connection handling by
 * implementing the [[com.github.mauricio.async.db.Connection]] trait and saving clients from having to implement
 * the "give back" part of pool management. This lets you do your job without having to worry
 * about managing and giving back connection objects to the pool.
 *
 * The downside of this is that you should not start transactions or any kind of long running process
 * in this object as the object will be sent back to the pool right after executing a query. If you
 * need to start transactions you will have to take an object from the pool, do it and then give it
 * back manually.
 *
 * @param factory
 * @param configuration
 */

class ConnectionPool[T <: Connection](
                      factory: ObjectFactory[T],
                      configuration: PoolConfiguration,
                      executionContext: ExecutionContext = ExecutorServiceUtils.FixedExecutionContext
                      )
  extends SingleThreadedAsyncObjectPool[T](factory, configuration)
  with Connection {

  /**
   *
   * Closes the pool, you should discard the object.
   *
   * @return
   */

  def disconnect: Future[Connection] = if ( this.isConnected ) {
    this.close.map(item => this)(executionContext)
  } else {
    Future.successful(this)
  }

  /**
   *
   * Always returns an empty map.
   *
   * @return
   */

  def connect: Future[Connection] = Future.successful(this)

  def isConnected: Boolean = !this.isClosed

  /**
   *
   * Picks one connection and runs this query against it. The query should be stateless, it should not
   * start transactions and should not leave anything to be cleaned up in the future. The behavior of this
   * object is undefined if you start a transaction from this method.
   *
   * @param query
   * @return
   */

  def sendQuery(query: String): Future[QueryResult] = {
    this.take.flatMap( {
      connection =>
        connection.sendQuery(query).andThen( {
          case c => this.giveBack(connection)
        })(executionContext)
    })(executionContext)
  }

  /**
   *
   * Picks one connection and runs this query against it. The query should be stateless, it should not
   * start transactions and should not leave anything to be cleaned up in the future. The behavior of this
   * object is undefined if you start a transaction from this method.
   *
   * @param query
   * @param values
   * @return
   */

  def sendPreparedStatement(query: String, values: Seq[Any] = List()): Future[QueryResult] = {
    this.take.flatMap( {
      connection =>
        connection.sendPreparedStatement(query, values).andThen( {
          case c => this.giveBack(connection)
        })(executionContext)
    })(executionContext)
  }

}
