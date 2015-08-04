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

import java.util.concurrent.atomic.{AtomicLong, AtomicReference}

import com.github.mauricio.async.db._
import com.github.mauricio.async.db.exceptions._
import com.github.mauricio.async.db.mysql.codec.{MySQLConnectionHandler, MySQLHandlerDelegate}
import com.github.mauricio.async.db.mysql.exceptions.MySQLException
import com.github.mauricio.async.db.mysql.message.client._
import com.github.mauricio.async.db.mysql.message.server._
import com.github.mauricio.async.db.mysql.util.CharsetMapper
import com.github.mauricio.async.db.pool.TimeoutScheduler
import com.github.mauricio.async.db.util.ChannelFutureTransformer.toFuture
import com.github.mauricio.async.db.util._
import io.netty.channel.{ChannelHandlerContext, EventLoopGroup}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

object MySQLConnection {
  final val Counter = new AtomicLong()
  final val MicrosecondsVersion = Version(5,6,0)
  final val log = Log.get[MySQLConnection]
}

class MySQLConnection(
                       configuration: Configuration,
                       charsetMapper: CharsetMapper = CharsetMapper.Instance,
                       group : EventLoopGroup = NettyUtils.DefaultEventLoopGroup,
                       implicit val executionContext : ExecutionContext = ExecutorServiceUtils.CachedExecutionContext
                       )
  extends MySQLHandlerDelegate
  with Connection
  with TimeoutScheduler
{

  import MySQLConnection.log

  // validate that this charset is supported
  charsetMapper.toInt(configuration.charset)

  private final val connectionCount = MySQLConnection.Counter.incrementAndGet()
  private final val connectionId = s"[mysql-connection-$connectionCount]"

  private final val connectionHandler = new MySQLConnectionHandler(
    configuration,
    charsetMapper,
    this,
    group,
    executionContext,
    connectionId)

  private final val connectionPromise = Promise[Connection]()
  private final val disconnectionPromise = Promise[Connection]()

  private val queryPromiseReference = new AtomicReference[Option[Promise[QueryResult]]](None)
  private var connected = false
  private var _lastException : Throwable = null
  private var serverVersion : Version = null

  def version = this.serverVersion
  def lastException : Throwable = this._lastException
  def count : Long = this.connectionCount

  override def eventLoopGroup : EventLoopGroup = group

  def connect: Future[Connection] = {
    this.connectionHandler.connect.onFailure {
      case e => this.connectionPromise.tryFailure(e)
    }

    this.connectionPromise.future
  }

  def close: Future[Connection] = {
    if ( this.isConnected ) {
      if (!this.disconnectionPromise.isCompleted) {
        val exception = new DatabaseException("Connection is being closed")
        exception.fillInStackTrace()
        this.failQueryPromise(exception)
        this.connectionHandler.clearQueryState
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
    log.error("Transport failure ", throwable)
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
    if ( !this.connectionPromise.isCompleted ) {
      log.debug("Connected to database")
      this.connectionPromise.success(this)
    } else {
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
      } else {
        log.warn("Received OK when not querying or connecting, not sure what this is")
      }
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

  override def switchAuthentication( message : AuthenticationSwitchRequest ) {
    this.connectionHandler.write(new AuthenticationSwitchResponse( configuration.password, message ))
  }

  def sendQuery(query: String): Future[QueryResult] = {
    this.validateIsReadyForQuery()
    val promise = Promise[QueryResult]()
    this.setQueryPromise(promise)
    this.connectionHandler.write(new QueryMessage(query))
    addTimeout(promise, configuration.queryTimeout)
    promise.future
  }

  private def failQueryPromise(t: Throwable) {
    this.clearQueryPromise.foreach {
      _.tryFailure(t)
    }
  }

  private def succeedQueryPromise(queryResult: QueryResult) {

    this.clearQueryPromise.foreach {
      _.success(queryResult)
    }

  }

  def isQuerying: Boolean = this.queryPromise.isDefined

  def onResultSet(resultSet: ResultSet, message: EOFMessage) {
    if (this.isQuerying) {
      this.succeedQueryPromise(
        new MySQLQueryResult(
          resultSet.size,
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
  override def onTimeout = disconnect

  def isConnected: Boolean = this.connectionHandler.isConnected

  def sendPreparedStatement(query: String, values: Seq[Any]): Future[QueryResult] = {
    this.validateIsReadyForQuery()
    val totalParameters = query.count( _ == '?')
    if ( values.length != totalParameters ) {
      throw new InsufficientParametersException(totalParameters, values)
    }
    val promise = Promise[QueryResult]()
    this.setQueryPromise(promise)
    this.connectionHandler.sendPreparedStatement(query, values)
    addTimeout(promise,configuration.queryTimeout)
    promise.future
  }


  override def toString: String = {
    "%s(%s,%d)".format(this.getClass.getName, this.connectionId, this.connectionCount)
  }

  private def validateIsReadyForQuery() {
    if ( isQuerying ) {
      throw new ConnectionStillRunningQueryException(this.connectionCount, false)
    }
  }

  private def queryPromise: Option[Promise[QueryResult]] = queryPromiseReference.get()

  private def setQueryPromise(promise: Promise[QueryResult]) {
    if (!this.queryPromiseReference.compareAndSet(None, Some(promise)))
      throw new ConnectionStillRunningQueryException(this.connectionCount, true)
  }

  private def clearQueryPromise : Option[Promise[QueryResult]] = {
    this.queryPromiseReference.getAndSet(None)
  }

}
