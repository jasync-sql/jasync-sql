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

import com.github.mauricio.async.db.column.ColumnDecoderRegistry
import com.github.mauricio.async.db.mysql.codec.{MySQLHandlerDelegate, MySQLConnectionHandler}
import com.github.mauricio.async.db.mysql.column.MySQLColumnDecoderRegistry
import com.github.mauricio.async.db.mysql.exceptions.MySQLException
import com.github.mauricio.async.db.mysql.message.client.{QueryMessage, ClientMessage, QuitMessage, HandshakeResponseMessage}
import com.github.mauricio.async.db.mysql.message.server.{EOFMessage, OkMessage, ErrorMessage, HandshakeMessage}
import com.github.mauricio.async.db.mysql.util.CharsetMapper
import com.github.mauricio.async.db.util.ChannelFutureTransformer.toFuture
import com.github.mauricio.async.db.util.Log
import com.github.mauricio.async.db.{Connection, ResultSet, QueryResult, Configuration}
import org.jboss.netty.channel._
import scala.concurrent.{ExecutionContext, Promise, Future}
import scala.util.{Failure, Success}

object MySQLConnection {
  val log = Log.get[MySQLConnection]
}

class MySQLConnection(
                       configuration: Configuration,
                       charsetMapper: CharsetMapper = CharsetMapper.Instance,
                       columnDecoderRegistry: ColumnDecoderRegistry = MySQLColumnDecoderRegistry.Instance
                       )
  extends MySQLHandlerDelegate
  with Connection
{

  import MySQLConnection.log

  // validate that this charset is supported
  charsetMapper.toInt(configuration.charset)

  private implicit val internalPool = ExecutionContext.fromExecutorService(configuration.workerPool)

  private final val connectionHandler = new MySQLConnectionHandler(configuration, charsetMapper, this, columnDecoderRegistry)

  private final val connectionPromise = Promise[Connection]()
  private final val disconnectionPromise = Promise[Connection]()
  private var queryPromise: Promise[QueryResult] = null
  private var connected = false

  def connect: Future[Connection] = {
    this.connectionHandler.connect.onFailure {
      case e => this.connectionPromise.tryFailure(e)
    }

    this.connectionPromise.future
  }

  def close: Future[Connection] = {

    if ( this.isConnected ) {
      if (!this.disconnectionPromise.isCompleted) {
        this.connectionHandler.write(QuitMessage).onComplete {
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
    log.debug("Connected to {}", ctx.getChannel.getRemoteAddress)
    this.connected = true
  }

  override def exceptionCaught(throwable: Throwable) {
    log.error("Transport failure", throwable)

    this.connectionPromise.tryFailure(throwable)
    this.failQueryPromise(throwable)
  }

  override def onOk(message: OkMessage) {
    log.debug("Received OK {}", message)
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
    log.debug("Received EOF message - {}", message)
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
    log.debug("Received handshake message - {}", message)

    this.connectionHandler.write(new HandshakeResponseMessage(
      configuration.username,
      configuration.charset,
      message.seed,
      message.authenticationMethod,
      database = configuration.database,
      password = configuration.password
    ))
  }

  override def onError(message: ErrorMessage) {
    log.error("Received an error message -> {}", message)
    val exception = new MySQLException(message)
    exception.fillInStackTrace()
    this.connectionPromise.tryFailure(exception)
    this.failQueryPromise(exception)
  }

  def sendQuery(query: String): Future[QueryResult] = {
    this.queryPromise = Promise[QueryResult]
    this.write(new QueryMessage(query))
    this.queryPromise.future
  }

  private def write(message: ClientMessage): ChannelFuture = {
    this.connectionHandler.write(message)
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

  private def isQuerying: Boolean = this.queryPromise != null

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

  def sendPreparedStatement(query: String, values: Seq[Any]): Future[QueryResult] = ???
}