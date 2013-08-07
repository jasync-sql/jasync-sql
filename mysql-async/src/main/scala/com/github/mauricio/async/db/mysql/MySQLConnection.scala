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

import com.github.mauricio.async.db._
import com.github.mauricio.async.db.exceptions.ConnectionStillRunningQueryException
import com.github.mauricio.async.db.mysql.codec.{MySQLHandlerDelegate, MySQLConnectionHandler}
import com.github.mauricio.async.db.mysql.exceptions.MySQLException
import com.github.mauricio.async.db.mysql.message.client._
import com.github.mauricio.async.db.mysql.message.server._
import com.github.mauricio.async.db.mysql.util.CharsetMapper
import com.github.mauricio.async.db.util.ChannelFutureTransformer.toFuture
import com.github.mauricio.async.db.util._
import java.util.concurrent.atomic.AtomicLong
import scala.Some
import scala.concurrent.{ExecutionContext, Promise, Future}
import scala.util.Failure
import scala.util.Success
import io.netty.channel.{EventLoopGroup, ChannelHandlerContext}

object MySQLConnection {
  final val log = Log.get[MySQLConnection]
  final val Counter = new AtomicLong()
  final val MicrosecondsVersion = Version(5,6,0)
}

class MySQLConnection(
                       configuration: Configuration,
                       charsetMapper: CharsetMapper = CharsetMapper.Instance,
                       group : EventLoopGroup = NettyUtils.DetaultEventLoopGroup,
                       executionContext : ExecutionContext = ExecutorServiceUtils.CachedExecutionContext
                       )
  extends MySQLHandlerDelegate
  with Connection
{

  import MySQLConnection.log

  // validate that this charset is supported
  charsetMapper.toInt(configuration.charset)

  private final val connectionCount = MySQLConnection.Counter.incrementAndGet()
  private implicit val internalPool = executionContext

  private final val connectionHandler = new MySQLConnectionHandler(configuration, charsetMapper, this, group, executionContext)

  private final val connectionPromise = Promise[Connection]()
  private final val disconnectionPromise = Promise[Connection]()

  private var queryPromise: Promise[QueryResult] = null
  private var connected = false
  private var _lastException : Throwable = null
  private var serverVersion : Version = null

  def version = this.serverVersion
  def lastException : Throwable = this._lastException
  def count : Long = this.connectionCount

  def connect: Future[Connection] = {
    this.connectionHandler.connect.onFailure {
      case e => this.connectionPromise.tryFailure(e)
    }

    this.connectionPromise.future
  }

  def close: Future[Connection] = {

    if ( this.isConnected ) {
      if (!this.disconnectionPromise.isCompleted) {
        this.connectionHandler.write(QuitMessage.Instance).onComplete {
          case Success(channelFuture) => {
            this.connectionHandler.disconnect.onComplete {
              case Success(closeFuture) => this.disconnectionPromise.trySuccess(this)
              case Failure(e) => this.disconnectionPromise.tryFailure(e)
            }
          }
          case Failure(exception) => this.disconnectionPromise.tryFailure(exception)
        }
      }
    }

    this.disconnectionPromise.future
  }

  override def connected(ctx: ChannelHandlerContext) {
    log.debug("Connected to {}", ctx.channel.remoteAddress)
    this.connected = true
  }

  override def exceptionCaught(throwable: Throwable) {
    log.error("Transport failure", throwable)
    setException(throwable)
  }

  override def onError(message: ErrorMessage) {
    log.error("Received an error message -> {}", message)
    val exception = new MySQLException(message)
    exception.fillInStackTrace()
    this.setException(exception)
  }

  private def setException( t : Throwable ) {
    this._lastException = t
    this.connectionPromise.tryFailure(t)
    this.failQueryPromise(t)
  }

  override def onOk(message: OkMessage) {
    this.connectionPromise.trySuccess(this)

    if (this.isQuerying) {
      this.succeedQueryPromise(
        new MySQLQueryResult(
          message.affectedRows,
          message.message,
          message.lastInsertId,
          message.statusFlags,
          message.warnings
        )
      )
    }

  }

  def onEOF(message: EOFMessage) {
    if (this.isQuerying) {
      this.succeedQueryPromise(
        new MySQLQueryResult(
          0,
          null,
          -1,
          message.flags,
          message.warningCount
        )
      )
    }
  }

  override def onHandshake(message: HandshakeMessage) {

    this.serverVersion = Version(message.serverVersion)

    this.connectionHandler.write(new HandshakeResponseMessage(
      configuration.username,
      configuration.charset,
      message.seed,
      message.authenticationMethod,
      database = configuration.database,
      password = configuration.password
    ))
  }

  def sendQuery(query: String): Future[QueryResult] = {
    this.validateIsReadyForQuery()
    val promise = Promise[QueryResult]
    this.queryPromise = promise
    this.connectionHandler.write(new QueryMessage(query))
    promise.future
  }

  private def failQueryPromise(t: Throwable) {

    if (this.isQuerying) {
      val promise = this.queryPromise
      this.queryPromise = null

      promise.tryFailure(t)
    }

  }

  private def succeedQueryPromise(queryResult: QueryResult) {

    if (this.isQuerying) {
      val promise = this.queryPromise
      this.queryPromise = null
      promise.success(queryResult)
    }

  }

  def isQuerying: Boolean = this.queryPromise != null && !this.queryPromise.isCompleted

  def onResultSet(resultSet: ResultSet, message: EOFMessage) {
    if (this.isQuerying) {
      this.succeedQueryPromise(
        new MySQLQueryResult(
          0,
          null,
          -1,
          message.flags,
          message.warningCount,
          Some(resultSet)
        )
      )
    }
  }

  def disconnect: Future[Connection] = this.close

  def isConnected: Boolean = this.connectionHandler.isConnected

  def sendPreparedStatement(query: String, values: Seq[Any]): Future[QueryResult] = {
    this.validateIsReadyForQuery()
    val promise = Promise[QueryResult]
    this.queryPromise = promise
    this.connectionHandler.write(new PreparedStatementMessage(query, values))
    promise.future
  }

  private def validateIsReadyForQuery() {
    if ( this.queryPromise != null && !this.queryPromise.isCompleted ) {
      throw new ConnectionStillRunningQueryException(this.connectionCount, false )
    }
  }

}
