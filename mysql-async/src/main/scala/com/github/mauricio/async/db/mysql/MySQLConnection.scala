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

import com.github.mauricio.async.db.Configuration
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import org.jboss.netty.bootstrap.ClientBootstrap
import scala.concurrent.{ExecutionContext, Promise, Future}
import org.jboss.netty.channel._
import java.net.InetSocketAddress
import com.github.mauricio.async.db.util.Log
import com.github.mauricio.async.db.mysql.message.server.{OkMessage, ErrorMessage, HandshakeMessage, ServerMessage}
import scala.annotation.switch
import com.github.mauricio.async.db.mysql.message.client.{QuitMessage, ClientMessage, HandshakeResponseMessage}
import com.github.mauricio.async.db.mysql.util.CharsetMapper
import com.github.mauricio.async.db.mysql.exceptions.MySQLException
import com.github.mauricio.async.db.util.ChannelFutureTransformer.toFuture
import scala.util.{Failure, Success}

object MySQLConnection {
  val log = Log.get[MySQLConnection]
}

class MySQLConnection(
                       configuration : Configuration,
                       charsetMapper : CharsetMapper = CharsetMapper.Instance )
  extends SimpleChannelHandler
  with LifeCycleAwareChannelHandler
{

  import MySQLConnection.log

  // validate that this charset is supported
  charsetMapper.toInt(configuration.charset)

  private implicit val internalPool = ExecutionContext.fromExecutorService(configuration.workerPool)
  private val factory = new NioClientSocketChannelFactory(
    configuration.bossPool,
    configuration.workerPool)

  private val bootstrap = new ClientBootstrap(this.factory)
  private val connectionPromise = Promise[MySQLConnection]()
  private var connected = false
  private var currentContext : ChannelHandlerContext = null

  def connect: Future[MySQLConnection] = {

    this.bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

      override def getPipeline(): ChannelPipeline = {
        Channels.pipeline(
          new MySQLFrameDecoder(configuration.charset),
          new MySQLOneToOneEncoder(configuration.charset, charsetMapper),
          MySQLConnection.this)
      }

    })

    this.bootstrap.setOption("child.tcpNoDelay", true)
    this.bootstrap.setOption("child.keepAlive", true)

    this.bootstrap.connect(new InetSocketAddress(configuration.host, configuration.port)).onFailure {
      case exception => this.connectionPromise.failure(exception)
    }

    this.connectionPromise.future
  }

  def close : Future[MySQLConnection] = {

    if ( this.currentContext.getChannel.isConnected ) {
      val promise = Promise[MySQLConnection]

      this.write( QuitMessage ).onComplete {
        case Success( channelFuture ) => {
          promise.success(this)
          if ( this.currentContext.getChannel.isConnected ) {
            this.currentContext.getChannel.close()
          }
        }
        case Failure(exception) => promise.failure(exception)
      }

      promise.future
    } else {
      Promise.successful(this).future
    }

  }

  override def channelConnected(ctx: ChannelHandlerContext, e: ChannelStateEvent): Unit = {
    log.debug("Connected to {}", ctx.getChannel.getRemoteAddress)
    this.connected = true
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    log.error("Transport failure", e.getCause)
    if ( !this.connectionPromise.isCompleted ) {
      this.connectionPromise.failure(e.getCause)
    }
  }

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {

    e.getMessage match {
      case m : ServerMessage => {
        (m.kind : @switch) match {
          case ServerMessage.ServerProtocolVersion => {
            this.onHandshake( m.asInstanceOf[HandshakeMessage] )
          }
          case ServerMessage.Ok => this.onOk(m.asInstanceOf[OkMessage])
          case ServerMessage.Error =>  this.onError(m.asInstanceOf[ErrorMessage])
        }
      }
    }

  }

  private def onOk( message : OkMessage ) {
    log.debug("Received OK {}", message)
    if ( !this.connectionPromise.isCompleted ) {
      this.connectionPromise.success(this)
    }
  }

  private def onHandshake( message : HandshakeMessage ) {
    log.debug("Received handshake message - {}", message)

    this.write(new HandshakeResponseMessage(
      configuration.username,
      configuration.charset,
      message.seed,
      message.authenticationMethod,
      database = configuration.database,
      password = configuration.password
    ))
  }

  private def onError( message : ErrorMessage ) {
    val exception = new MySQLException(message)
    exception.fillInStackTrace()
    if ( !this.connectionPromise.isCompleted ) {
      this.connectionPromise.failure(exception)
    }
  }

  def beforeAdd(ctx: ChannelHandlerContext) {}

  def beforeRemove(ctx: ChannelHandlerContext) {}

  def afterAdd(ctx: ChannelHandlerContext) {
    this.currentContext = ctx
  }

  def afterRemove(ctx: ChannelHandlerContext) {
    this.currentContext = null
  }

  private def write( message : ClientMessage ) : ChannelFuture =  {
    this.currentContext.getChannel.write(message)
  }

}