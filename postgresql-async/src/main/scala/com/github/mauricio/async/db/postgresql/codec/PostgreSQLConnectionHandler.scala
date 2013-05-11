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

package com.github.mauricio.async.db.postgresql.codec

import com.github.mauricio.async.db.Configuration
import com.github.mauricio.async.db.column.{ColumnDecoderRegistry, ColumnEncoderRegistry}
import com.github.mauricio.async.db.postgresql.exceptions._
import com.github.mauricio.async.db.postgresql.messages.backend._
import com.github.mauricio.async.db.postgresql.messages.frontend._
import com.github.mauricio.async.db.util.ChannelFutureTransformer.toFuture
import com.github.mauricio.async.db.util.Log
import java.net.InetSocketAddress
import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel._
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import scala.annotation.switch
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

object PostgreSQLConnectionHandler {
  final val log = Log.get[PostgreSQLConnectionHandler]
}

class PostgreSQLConnectionHandler
(
  configuration: Configuration,
  encoderRegistry: ColumnEncoderRegistry,
  decoderRegistry: ColumnDecoderRegistry,
  connectionDelegate : PostgreSQLConnectionDelegate
  )
  extends SimpleChannelHandler
  with LifeCycleAwareChannelHandler
{

  import PostgreSQLConnectionHandler.log

  private val properties = List(
    "user" -> configuration.username,
    "database" -> configuration.database,
    "application_name" -> "Netty-PostgreSQL-driver-0.1.0",
    "client_encoding" -> configuration.charset.name(),
    "DateStyle" -> "ISO",
    "extra_float_digits" -> "2")

  private final val factory = new NioClientSocketChannelFactory(
    configuration.bossPool,
    configuration.workerPool,
    1)

  private final implicit val executionContext = ExecutionContext.fromExecutorService(configuration.workerPool)
  private final val bootstrap = new ClientBootstrap(this.factory)
  private final val connectionFuture = Promise[PostgreSQLConnectionHandler]()
  private final val disconnectionPromise = Promise[PostgreSQLConnectionHandler]()
  private var processData : ProcessData = null

  private var currentContext : ChannelHandlerContext = null

  def connect: Future[PostgreSQLConnectionHandler] = {

    this.bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

      override def getPipeline(): ChannelPipeline = {
        Channels.pipeline(
          new MessageDecoder(configuration.charset, configuration.maximumMessageSize),
          new MessageEncoder(configuration.charset, encoderRegistry),
          PostgreSQLConnectionHandler.this)
      }

    })

    this.bootstrap.setOption("child.tcpNoDelay", true)
    this.bootstrap.setOption("child.keepAlive", true)

    this.bootstrap.connect(new InetSocketAddress(configuration.host, configuration.port)).onFailure {
      case e => connectionFuture.tryFailure(e)
    }

    this.connectionFuture.future
  }

  def disconnect: Future[PostgreSQLConnectionHandler] = {

    if ( this.isConnected ) {
      this.currentContext.getChannel.write(CloseMessage).onComplete {
        case Success(writeFuture) => writeFuture.getChannel.close().onComplete {
          case Success(closeFuture) => this.disconnectionPromise.trySuccess(this)
          case Failure(e) => this.disconnectionPromise.tryFailure(e)
        }
        case Failure(e) => this.disconnectionPromise.tryFailure(e)
      }
    }

    this.disconnectionPromise.future
  }

  def isConnected: Boolean = {
    if (this.currentContext != null) {
      this.currentContext.getChannel.isConnected
    } else {
      false
    }
  }

  override def channelConnected(ctx: ChannelHandlerContext, e: ChannelStateEvent): Unit = {
    e.getChannel().write(new StartupMessage(this.properties))
  }

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent): Unit = {

    e.getMessage() match {

      case m: ServerMessage => {

        (m.kind : @switch) match {
          case ServerMessage.BackendKeyData => {
            this.processData = m.asInstanceOf[ProcessData]
          }
          case ServerMessage.BindComplete => {
          }
          case ServerMessage.Authentication => {
            connectionDelegate.onAuthenticationResponse(m.asInstanceOf[AuthenticationMessage])
          }
          case ServerMessage.CommandComplete => {
            connectionDelegate.onCommandComplete(m.asInstanceOf[CommandCompleteMessage])
          }
          case ServerMessage.CloseComplete => {
          }
          case ServerMessage.DataRow => {
            connectionDelegate.onDataRow(m.asInstanceOf[DataRowMessage])
          }
          case ServerMessage.Error => {
            connectionDelegate.onError(m.asInstanceOf[ErrorMessage])
          }
          case ServerMessage.EmptyQueryString => {
            val exception = new QueryMustNotBeNullOrEmptyException(null)
            exception.fillInStackTrace()
            connectionDelegate.onError(exception)
          }
          case ServerMessage.NoData => {
          }
          case ServerMessage.Notice => {
          }
          case ServerMessage.ParameterStatus => {
            connectionDelegate.onParameterStatus(m.asInstanceOf[ParameterStatusMessage])
          }
          case ServerMessage.ParseComplete => {
            log.debug("Parse complete received - {}", m)
          }
          case ServerMessage.ReadyForQuery => {
            connectionDelegate.onReadyForQuery()
          }
          case ServerMessage.RowDescription => {
            log.debug("Row description received - {}", m)
            connectionDelegate.onRowDescription(m.asInstanceOf[RowDescriptionMessage])
          }
          case _ => {
            val exception = new IllegalStateException("Handler not implemented for message %s".format(m.kind))
            exception.fillInStackTrace()
            connectionDelegate.onError(exception)
          }
        }

      }
      case _ => {
        log.error("Unknown message type - {}", e.getMessage)
        val exception = new IllegalArgumentException("Unknown message type - %s".format(e.getMessage()))
        exception.fillInStackTrace()
        connectionDelegate.onError(exception)
      }

    }

  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    connectionDelegate.onError(e.getCause)
  }

  override def channelDisconnected(ctx: ChannelHandlerContext, e: ChannelStateEvent): Unit = {
    log.info("Connection disconnected - {}", ctx.getChannel.getRemoteAddress)
  }

  def beforeAdd(ctx: ChannelHandlerContext) {
    this.currentContext = ctx
  }

  def afterAdd(ctx: ChannelHandlerContext) {}

  def beforeRemove(ctx: ChannelHandlerContext) {}

  def afterRemove(ctx: ChannelHandlerContext) {}

  def write( message : ClientMessage ) {
    this.currentContext.getChannel.write(message)
  }

}