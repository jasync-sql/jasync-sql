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

package com.github.mauricio.async.db.postgresql

import com.github.mauricio.async.db.postgresql.exceptions.{MissingCredentialInformationException, NotConnectedException, GenericDatabaseException}
import com.github.mauricio.async.db.util.{Log, ExecutorServiceUtils}
import com.github.mauricio.async.db.{Configuration, QueryResult, Connection}
import com.github.mauricio.postgresql.MessageEncoder
import concurrent.{Future, Promise}
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import messages.backend._
import messages.frontend._
import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel._
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import org.jboss.netty.logging.{Slf4JLoggerFactory, InternalLoggerFactory}
import scala.Some
import scala.collection.JavaConversions._
import com.github.mauricio.async.db.postgresql.column.{DefaultColumnDecoderRegistry, ColumnDecoderRegistry, DefaultColumnEncoderRegistry, ColumnEncoderRegistry}

object DatabaseConnectionHandler {
  val log = Log.get[DatabaseConnectionHandler]
  val Name = "Netty-PostgreSQL-driver-0.0.1"
  InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory())
}

class DatabaseConnectionHandler
(
  configuration: Configuration = Configuration.Default,
  encoderRegistry : ColumnEncoderRegistry = DefaultColumnEncoderRegistry.Instance,
  decoderRegistry : ColumnDecoderRegistry = DefaultColumnDecoderRegistry.Instance
  ) extends SimpleChannelHandler with Connection {

  import DatabaseConnectionHandler._

  private val properties = List(
    "user" -> configuration.username,
    "database" -> configuration.database,
    "application_name" -> DatabaseConnectionHandler.Name,
    "client_encoding" -> configuration.charset.name(),
    "DateStyle" -> "ISO",
    "extra_float_digits" -> "2")

  private var readyForQuery = false
  private val parameterStatus = new ConcurrentHashMap[String, String]()
  private val parsedStatements = new ConcurrentHashMap[String, Array[ColumnData]]()
  private var _processData: Option[ProcessData] = None
  private var authenticated = false

  private val factory = new NioClientSocketChannelFactory(
    ExecutorServiceUtils.CachedThreadPool,
    ExecutorServiceUtils.CachedThreadPool)

  private val bootstrap = new ClientBootstrap(this.factory)
  private val connectionFuture = Promise[Map[String, String]]()

  private var connected = false
  private var queryPromise: Option[Promise[QueryResult]] = None
  private var currentQuery: Option[MutableQuery] = None
  private var currentPreparedStatement: Option[String] = None
  private var _currentChannel: Option[Channel] = None

  def isReadyForQuery: Boolean = this.readyForQuery

  def connect: Future[Map[String, String]] = {

    this.bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

      override def getPipeline(): ChannelPipeline = {
        Channels.pipeline(
          new MessageDecoder(configuration.charset),
          new MessageEncoder(configuration.charset, encoderRegistry),
          DatabaseConnectionHandler.this)
      }

    })

    this.bootstrap.setOption("child.tcpNoDelay", true)
    this.bootstrap.setOption("child.keepAlive", true)

    this.bootstrap.connect(new InetSocketAddress(configuration.host, configuration.port)).addListener(new ChannelFutureListener {
      def operationComplete(future: ChannelFuture) {

        if (future.isSuccess) {
          connected = true
          _currentChannel = Some(future.getChannel)
        } else {
          connectionFuture.failure(future.getCause)
        }

      }
    })

    this.connectionFuture.future
  }

  override def disconnect: Future[Connection] = {

    val closingPromise = Promise[Connection]()

    if (this.currentChannel.isConnected) {
      this.currentChannel.write(CloseMessage.Instance).addListener(new ChannelFutureListener {
        def operationComplete(future: ChannelFuture) {

          if (future.getCause != null) {
            closingPromise.failure(future.getCause)
          } else {
            if ( future.getChannel.isOpen ) {
              future.getChannel.close().addListener(new ChannelFutureListener {
                def operationComplete(internalFuture: ChannelFuture) {
                  if (internalFuture.isSuccess) {
                    closingPromise.success(DatabaseConnectionHandler.this)
                  } else {
                    closingPromise.failure(internalFuture.getCause)
                  }
                }
              })
            } else {
              closingPromise.success(DatabaseConnectionHandler.this)
            }
          }
        }
      })

    } else {
      closingPromise.success(this)
    }

    closingPromise.future
  }

  override def isConnected: Boolean = {
    this.connected
  }

  def parameterStatuses: scala.collection.immutable.Map[String, String] = this.parameterStatus.toMap

  def processData: Option[ProcessData] = {
    _processData
  }

  override def channelConnected(ctx: ChannelHandlerContext, e: ChannelStateEvent): Unit = {
    this.connected = true
    e.getChannel().write(new StartupMessage(this.properties))
  }

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent): Unit = {

    e.getMessage() match {

      case m: Message => {

        m.name match {
          case Message.BackendKeyData => {
            this._processData = Some(m.asInstanceOf[ProcessData])
          }
          case Message.BindComplete => {
            log.debug("Finished binding statement - {}", this.currentPreparedStatement)
          }
          case Message.Authentication => {
            this.onAuthenticationResponse(ctx.getChannel, m.asInstanceOf[AuthenticationMessage])
          }
          case Message.CommandComplete => {
            this.onCommandComplete(m.asInstanceOf[CommandCompleteMessage])
          }
          case Message.CloseComplete => {
            log.debug("Successfully closed portal for [{}]", this.currentPreparedStatement)
          }
          case Message.DataRow => {
            this.onDataRow(m.asInstanceOf[DataRowMessage])
          }
          case Message.Error => {
            this.onError(m.asInstanceOf[ErrorMessage])
          }
          case Message.NoData => {
            log.debug("Statement response does not contain any data")
          }
          case Message.Notice => {
            log.info("notice -> {}", m.asInstanceOf[NoticeMessage])
          }
          case Message.ParameterStatus => {
            this.onParameterStatus(m.asInstanceOf[ParameterStatusMessage])
          }
          case Message.ParseComplete => {
            log.debug("Finished parsing statement")
          }
          case Message.ReadyForQuery => {
            this.onReadyForQuery
          }
          case Message.RowDescription => {
            this.onRowDescription(m.asInstanceOf[RowDescriptionMessage])
          }
          case _ => {
            throw new IllegalStateException("Handler not implemented for message %s".format(m.name))
          }
        }

      }
      case _ => {
        log.error("Unknown message type {}", e.getMessage)
        throw new IllegalArgumentException("Unknown message type - %s".format(e.getMessage()))
      }

    }

  }

  override def sendQuery(query: String): Future[QueryResult] = {
    this.readyForQuery = false
    this.queryPromise = Option(Promise[QueryResult]())
    this.currentChannel.write(new QueryMessage(query))
    this.queryPromise.get.future
  }

  override def sendPreparedStatement(query: String, values: Array[Any] = Array.empty[Any]): Future[QueryResult] = {
    this.readyForQuery = false
    this.queryPromise = Some(Promise[QueryResult]())

    var paramsCount = 0

    val realQuery = if (query.contains("?")) {
      query.foldLeft(new StringBuilder()) {
        (builder, char) =>
          if (char == '?') {
            paramsCount += 1
            builder.append("$" + paramsCount)
          } else {
            builder.append(char)
          }
          builder
      }.toString()
    } else {
      query
    }

    this.currentPreparedStatement = Some(realQuery)

    if (!this.isParsed(realQuery)) {
      log.debug("MutableQuery is not parsed yet -> {}", realQuery)
      this.currentChannel.write(new PreparedStatementOpeningMessage(realQuery, values, this.encoderRegistry))
    } else {
      this.currentQuery = Some(new MutableQuery(this.parsedStatements.get(realQuery), configuration.charset, this.decoderRegistry))
      this.currentChannel.write(new PreparedStatementExecuteMessage(realQuery, values, this.encoderRegistry))
    }

    this.queryPromise.get.future
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    this.setErrorOnFutures(e.getCause)
  }

  private def setErrorOnFutures(e: Throwable) {

    log.error("Error on connection", e)

    if (!this.connectionFuture.isCompleted) {
      this.connectionFuture.failure(e)
      this.disconnect
    } else {
      if (this.queryPromise.isDefined) {
        log.error("Setting error on future {}", this.queryPromise.get)
        this.queryPromise.get.failure(e)
        this.queryPromise = None
        this.currentPreparedStatement = None
      }
    }

  }

  override def channelDisconnected(ctx: ChannelHandlerContext, e: ChannelStateEvent): Unit = {
    log.warn("Connection disconnected - {}", ctx.getChannel.getRemoteAddress)
    this.connected = false
  }

  private def onReadyForQuery {
    this.readyForQuery = true

    if (!this.connectionFuture.isCompleted) {
      this.connectionFuture.success(this.parameterStatus.toMap)
    }
  }

  private def onError(m: ErrorMessage) {
    log.error("Error with message -> {}", m)

    val error = new GenericDatabaseException(m)
    error.fillInStackTrace()

    this.setErrorOnFutures(error)
  }

  private def onCommandComplete(m: CommandCompleteMessage) {

    if (this.queryPromise.isDefined) {

      val queryResult = if (this.currentQuery.isDefined) {
        new QueryResult(m.rowsAffected, m.statusMessage, Some(this.currentQuery.get))
      } else {
        new QueryResult(m.rowsAffected, m.statusMessage, None)
      }

      this.queryPromise.get.success(queryResult)
      this.queryPromise = None
      this.currentPreparedStatement = None

    }
  }

  private def onParameterStatus(m: ParameterStatusMessage) {
    this.parameterStatus.put(m.key, m.value)
  }

  private def onDataRow(m: DataRowMessage) {
    this.currentQuery.get.addRawRow(m.values)
  }

  private def onRowDescription(m: RowDescriptionMessage) {
    log.debug("received query description {}", m)
    this.currentQuery = Option(new MutableQuery(m.columnDatas, configuration.charset, this.decoderRegistry))

    log.debug("Current prepared statement is {}", this.currentPreparedStatement)

    if (this.currentPreparedStatement.isDefined) {
      this.parsedStatements.put(this.currentPreparedStatement.get, m.columnDatas)
    }
  }

  private def isParsed(query: String): Boolean = {
    this.parsedStatements.containsKey(query)
  }

  private def onAuthenticationResponse(channel: Channel, message: AuthenticationMessage) {

    message match {
      case m: AuthenticationOkMessage => {
        log.debug("Successfully logged in to database")
        this.authenticated = true
      }
      case m: AuthenticationChallengeCleartextMessage => {
        channel.write(this.credential(m))
      }
      case m: AuthenticationChallengeMD5 => {
        channel.write(this.credential(m))
      }
    }

  }

  private def currentChannel: Channel = {
    if (this._currentChannel.isDefined) {
      return this._currentChannel.get
    } else {
      throw new NotConnectedException("This object is not connected")
    }
  }

  private def credential(authenticationMessage: AuthenticationChallengeMessage): CredentialMessage = {
    if (configuration.username != null && configuration.password.isDefined) {
      new CredentialMessage(
        configuration.username,
        configuration.password.get,
        authenticationMessage.challengeType,
        authenticationMessage.salt
      )
    } else {
      throw new MissingCredentialInformationException(
        this.configuration.username,
        this.configuration.password,
        authenticationMessage.challengeType)
    }
  }

}