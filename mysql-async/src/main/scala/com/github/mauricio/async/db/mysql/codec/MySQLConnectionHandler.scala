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

package com.github.mauricio.async.db.mysql.codec

import com.github.mauricio.async.db.Configuration
import com.github.mauricio.async.db.mysql.message.client.{QueryMessage, ClientMessage}
import com.github.mauricio.async.db.mysql.message.server._
import com.github.mauricio.async.db.mysql.util.CharsetMapper
import com.github.mauricio.async.db.util.ChannelFutureTransformer.toFuture
import com.github.mauricio.async.db.util.Log
import java.net.InetSocketAddress
import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel._
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import scala.annotation.switch
import scala.concurrent.{ExecutionContext, Promise, Future}
import scala.collection.mutable.ArrayBuffer
import com.github.mauricio.async.db.mysql.message.server.HandshakeMessage
import com.github.mauricio.async.db.mysql.message.server.ErrorMessage
import com.github.mauricio.async.db.mysql.message.client.QueryMessage
import com.github.mauricio.async.db.mysql.message.server.OkMessage

object MySQLConnectionHandler {
  val log = Log.get[MySQLConnectionHandler]
}

class MySQLConnectionHandler(
                              configuration : Configuration,
                              charsetMapper : CharsetMapper,
                              handlerDelegate : MySQLHandlerDelegate
                              )
  extends SimpleChannelHandler
  with LifeCycleAwareChannelHandler {

  import MySQLConnectionHandler.log

  private implicit val internalPool = ExecutionContext.fromExecutorService(configuration.workerPool)

  private final val factory = new NioClientSocketChannelFactory(
    configuration.bossPool,
    configuration.workerPool)

  private final val bootstrap = new ClientBootstrap(this.factory)
  private final val connectionPromise = Promise[MySQLConnectionHandler]
  private final val decoder = new MySQLFrameDecoder(configuration.charset)
  private final val encoder = new MySQLOneToOneEncoder(configuration.charset, charsetMapper)
  private final val currentColumns = new ArrayBuffer[ColumnDefinitionMessage]()
  private final val currentRows = new ArrayBuffer[ResultSetRowMessage]()
  private var currentContext : ChannelHandlerContext = null

  def connect: Future[MySQLConnectionHandler] = {

    this.bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

      override def getPipeline(): ChannelPipeline = {
        Channels.pipeline(
          decoder,
          encoder,
          MySQLConnectionHandler.this)
      }

    })

    this.bootstrap.setOption("child.tcpNoDelay", true)
    this.bootstrap.setOption("child.keepAlive", true)

    this.bootstrap.connect(new InetSocketAddress(configuration.host, configuration.port)).onFailure {
      case exception => this.connectionPromise.failure(exception)
    }

    this.connectionPromise.future
  }

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {

    e.getMessage match {
      case m : ServerMessage => {
        (m.kind : @switch) match {
          case ServerMessage.ServerProtocolVersion => {
            handlerDelegate.onHandshake( m.asInstanceOf[HandshakeMessage] )
          }
          case ServerMessage.Ok => {
            this.clearQueryState
            handlerDelegate.onOk(m.asInstanceOf[OkMessage])
          }
          case ServerMessage.Error =>  {
            this.clearQueryState
            handlerDelegate.onError(m.asInstanceOf[ErrorMessage])
          }
          case ServerMessage.EOF => {
            this.clearQueryState
            handlerDelegate.onEOF(m.asInstanceOf[EOFMessage])
          }
          case ServerMessage.ColumnDefinition => {
            log.debug("Received column definition - {}", m)
            this.currentColumns += m.asInstanceOf[ColumnDefinitionMessage]
          }
          case ServerMessage.ColumnDefinitionFinished => {
            log.debug("Column processing finished, waiting for rows now -> {}", m)
          }
          case ServerMessage.Row => {
            log.debug("Received row - {}", m)
            this.currentRows += m.asInstanceOf[ResultSetRowMessage]
          }
        }
      }
    }

  }

  override def channelConnected(ctx: ChannelHandlerContext, e: ChannelStateEvent): Unit = {
    handlerDelegate.connected( ctx )
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    if ( !this.connectionPromise.isCompleted ) {
      this.connectionPromise.failure(e.getCause)
    }
    handlerDelegate.exceptionCaught( e.getCause )
  }

  def beforeAdd(ctx: ChannelHandlerContext) {}

  def beforeRemove(ctx: ChannelHandlerContext) {}

  def afterAdd(ctx: ChannelHandlerContext) {
    this.currentContext = ctx
  }

  def afterRemove(ctx: ChannelHandlerContext) {}

  def write( message : ClientMessage ) : ChannelFuture =  {
    if ( message.kind == ClientMessage.Query ) {
      this.decoder.queryProcessStarted()
    }
    this.currentContext.getChannel.write(message)
  }

  def disconnect : ChannelFuture = {
    this.currentContext.getChannel.close()
  }

  private def clearQueryState {
    this.currentColumns.clear()
    this.currentRows.clear()
  }

}
