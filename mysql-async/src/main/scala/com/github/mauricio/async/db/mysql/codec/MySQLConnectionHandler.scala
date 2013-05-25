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
import com.github.mauricio.async.db.general.MutableResultSet
import com.github.mauricio.async.db.mysql.binary.BinaryRowDecoder
import com.github.mauricio.async.db.mysql.message.client._
import com.github.mauricio.async.db.mysql.message.server._
import com.github.mauricio.async.db.mysql.util.CharsetMapper
import com.github.mauricio.async.db.util.ChannelFutureTransformer.toFuture
import com.github.mauricio.async.db.util.Log
import java.net.InetSocketAddress
import java.nio.ByteOrder
import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.buffer.HeapChannelBufferFactory
import org.jboss.netty.channel._
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import scala.annotation.switch
import scala.collection.mutable.{ArrayBuffer, HashMap}
import scala.concurrent.{ExecutionContext, Promise, Future}

object MySQLConnectionHandler {
  val log = Log.get[MySQLConnectionHandler]
}

class MySQLConnectionHandler(
                              configuration: Configuration,
                              charsetMapper: CharsetMapper,
                              handlerDelegate: MySQLHandlerDelegate
                              )
  extends SimpleChannelHandler
  with LifeCycleAwareChannelHandler {


  private implicit val internalPool = ExecutionContext.fromExecutorService(configuration.workerPool)

  private final val factory = new NioClientSocketChannelFactory(
    configuration.bossPool,
    configuration.workerPool,
    1)

  private final val bootstrap = new ClientBootstrap(this.factory)
  private final val connectionPromise = Promise[MySQLConnectionHandler]
  private final val decoder = new MySQLFrameDecoder(configuration.charset)
  private final val encoder = new MySQLOneToOneEncoder(configuration.charset, charsetMapper)
  private final val currentParameters = new ArrayBuffer[ColumnDefinitionMessage]()
  private final val currentColumns = new ArrayBuffer[ColumnDefinitionMessage]()
  private final val parsedStatements = new HashMap[String,PreparedStatementHolder]()
  private final val binaryRowDecoder = new BinaryRowDecoder()

  private var currentPreparedStatementHolder : PreparedStatementHolder = null
  private var currentPreparedStatement : PreparedStatementMessage = null
  private var currentQuery : MutableResultSet[ColumnDefinitionMessage] = null
  private var currentContext: ChannelHandlerContext = null

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

    this.bootstrap.setOption("bufferFactory", HeapChannelBufferFactory.getInstance(ByteOrder.LITTLE_ENDIAN));

    this.bootstrap.connect(new InetSocketAddress(configuration.host, configuration.port)).onFailure {
      case exception => this.connectionPromise.tryFailure(exception)
    }

    this.connectionPromise.future
  }

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {

    //log.debug("Message received {}", e.getMessage)

    e.getMessage match {
      case m: ServerMessage => {
        (m.kind: @switch) match {
          case ServerMessage.ServerProtocolVersion => {
            handlerDelegate.onHandshake(m.asInstanceOf[HandshakeMessage])
          }
          case ServerMessage.Ok => {
            this.clearQueryState
            handlerDelegate.onOk(m.asInstanceOf[OkMessage])
          }
          case ServerMessage.Error => {
            this.clearQueryState
            handlerDelegate.onError(m.asInstanceOf[ErrorMessage])
          }
          case ServerMessage.EOF => {

            val resultSet = this.currentQuery
            this.clearQueryState

            if ( resultSet != null ) {
              handlerDelegate.onResultSet( resultSet, m.asInstanceOf[EOFMessage] )
            } else {
              handlerDelegate.onEOF(m.asInstanceOf[EOFMessage])
            }

          }
          case ServerMessage.ColumnDefinition => {
            val message = m.asInstanceOf[ColumnDefinitionMessage]

            if ( currentPreparedStatementHolder != null && this.currentPreparedStatementHolder.needsAny ) {
              this.currentPreparedStatementHolder.add(message)
            }

            this.currentColumns += message
          }
          case ServerMessage.ColumnDefinitionFinished => {
            this.onColumnDefinitionFinished()
          }
          case ServerMessage.PreparedStatementPrepareResponse => {
            this.onPreparedStatementPrepareResponse(m.asInstanceOf[PreparedStatementPrepareResponse])
          }
          case ServerMessage.Row => {
            val message = m.asInstanceOf[ResultSetRowMessage]
            val items = new Array[Any](message.size)

            var x = 0
            while ( x < message.size ) {
              items(x) = if ( message(x) == null ) {
                null
              } else {
                val columnDescription = this.currentQuery.columnTypes(x)
                columnDescription.textDecoder.decode(message(x), configuration.charset)
              }
              x += 1
            }

            this.currentQuery.addRow(items)
          }
          case ServerMessage.BinaryRow => {
            val message = m.asInstanceOf[BinaryRowMessage]
            this.currentQuery.addRow( this.binaryRowDecoder.decode(message.buffer, this.currentColumns ))
          }
          case ServerMessage.ParamProcessingFinished => {
          }
          case ServerMessage.ParamAndColumnProcessingFinished => {
            this.onColumnDefinitionFinished()
          }
        }
      }
    }

  }

  override def channelConnected(ctx: ChannelHandlerContext, e: ChannelStateEvent): Unit = {
    handlerDelegate.connected(ctx)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    if (!this.connectionPromise.isCompleted) {
      this.connectionPromise.failure(e.getCause)
    }
    handlerDelegate.exceptionCaught(e.getCause)
  }

  def beforeAdd(ctx: ChannelHandlerContext) {}

  def beforeRemove(ctx: ChannelHandlerContext) {}

  def afterAdd(ctx: ChannelHandlerContext) {
    this.currentContext = ctx
  }

  def afterRemove(ctx: ChannelHandlerContext) {}

  def write( message : QueryMessage ) : ChannelFuture = {
    this.decoder.queryProcessStarted()
    this.currentContext.getChannel.write(message)
  }

  def write( message : PreparedStatementMessage )  {

    this.currentColumns.clear()
    this.currentParameters.clear()

    this.currentPreparedStatement = message

    this.parsedStatements.get(message.statement) match {
      case Some( item ) => {
        this.executePreparedStatement(item.statementId, item.columns.size, message.values, item.parameters)
      }
      case None => {
        decoder.preparedStatementPrepareStarted()
        this.currentContext.getChannel.write( new PreparedStatementPrepareMessage(message.statement) )
      }
    }
  }

  def write( message : HandshakeResponseMessage ) : ChannelFuture = {
    this.currentContext.getChannel.write(message)
  }

  def write( message : QuitMessage ) : ChannelFuture = {
    this.currentContext.getChannel.write(message)
  }

  def disconnect: ChannelFuture = this.currentContext.getChannel.close()

  private def clearQueryState {
    this.currentColumns.clear()
    this.currentParameters.clear()
    this.currentQuery = null
  }

  def isConnected : Boolean = {
    if ( this.currentContext != null ) {
      this.currentContext.getChannel.isConnected
    } else {
      false
    }
  }

  private def executePreparedStatement( statementId : Array[Byte], columnsCount : Int, values : Seq[Any], parameters : Seq[ColumnDefinitionMessage] ) {
    decoder.preparedStatementExecuteStarted(columnsCount, parameters.size)
    this.currentColumns.clear()
    this.currentParameters.clear()
    this.currentContext.getChannel.write(new PreparedStatementExecuteMessage( statementId, values, parameters ))
  }

  private def onPreparedStatementPrepareResponse( message : PreparedStatementPrepareResponse ) {
    this.currentPreparedStatementHolder = new PreparedStatementHolder( this.currentPreparedStatement.statement, message)
  }

  def onColumnDefinitionFinished() {
    this.currentQuery = new MutableResultSet[ColumnDefinitionMessage](
      this.currentColumns
    )

    if ( this.currentPreparedStatementHolder != null ) {
      this.parsedStatements.put( this.currentPreparedStatementHolder.statement, this.currentPreparedStatementHolder )
      this.executePreparedStatement(
        this.currentPreparedStatementHolder.statementId,
        this.currentPreparedStatementHolder.columns.size,
        this.currentPreparedStatement.values,
        this.currentPreparedStatementHolder.parameters
      )
      this.currentPreparedStatementHolder = null
      this.currentPreparedStatement = null
    }
  }

}
