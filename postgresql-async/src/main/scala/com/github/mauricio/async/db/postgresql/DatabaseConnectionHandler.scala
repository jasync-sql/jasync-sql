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

import com.github.mauricio.async.db.column.{ColumnEncoderRegistry, ColumnDecoderRegistry}
import com.github.mauricio.async.db.general.MutableResultSet
import com.github.mauricio.async.db.postgresql.column.{PostgreSQLColumnDecoderRegistry, PostgreSQLColumnEncoderRegistry}
import com.github.mauricio.async.db.postgresql.exceptions._
import com.github.mauricio.async.db.util.Log
import com.github.mauricio.async.db.{Configuration, QueryResult, Connection}
import concurrent.{Future, Promise}
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.{AtomicReference, AtomicLong}
import messages.backend._
import messages.frontend._
import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel._
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import org.jboss.netty.logging.{Slf4JLoggerFactory, InternalLoggerFactory}
import scala.Some
import scala.annotation.switch
import scala.collection.JavaConversions._

object DatabaseConnectionHandler {
  val log = Log.get[DatabaseConnectionHandler]
  val Name = "Netty-PostgreSQL-driver-0.1.0"
  InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory())
  val Counter = new AtomicLong()
}

class DatabaseConnectionHandler
(
  configuration: Configuration = Configuration.Default,
  encoderRegistry: ColumnEncoderRegistry = PostgreSQLColumnEncoderRegistry.Instance,
  decoderRegistry: ColumnDecoderRegistry = PostgreSQLColumnDecoderRegistry.Instance
  ) extends SimpleChannelHandler with Connection {

  import DatabaseConnectionHandler._

  private val currentCount = Counter.incrementAndGet()
  private val properties = List(
    "user" -> configuration.username,
    "database" -> configuration.database,
    "application_name" -> DatabaseConnectionHandler.Name,
    "client_encoding" -> configuration.charset.name(),
    "DateStyle" -> "ISO",
    "extra_float_digits" -> "2")

  private var readyForQuery = false
  private val parameterStatus = new ConcurrentHashMap[String, String]()
  private val parsedStatements = new ConcurrentHashMap[String, Array[PostgreSQLColumnData]]()
  private var _processData: Option[ProcessData] = None
  private var authenticated = false

  private val factory = new NioClientSocketChannelFactory(
    configuration.bossPool,
    configuration.workerPool)

  private val bootstrap = new ClientBootstrap(this.factory)
  private val connectionFuture = Promise[Connection]()

  private var connected = false
  private var recentError = false
  private val queryPromiseReference = new AtomicReference[Option[Promise[QueryResult]]](None)
  private var currentQuery: Option[MutableResultSet[PostgreSQLColumnData]] = None
  private var currentPreparedStatement: Option[String] = None
  private var _currentChannel: Option[Channel] = None

  def isReadyForQuery: Boolean = this.readyForQuery

  def connect: Future[Connection] = {

    this.bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

      override def getPipeline(): ChannelPipeline = {
        Channels.pipeline(
          new MessageDecoder(configuration.charset, configuration.maximumMessageSize),
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
      this.currentChannel.write(CloseMessage).addListener(new ChannelFutureListener {
        def operationComplete(future: ChannelFuture) {

          if (future.getCause != null) {
            closingPromise.failure(future.getCause)
          } else {
            if (future.getChannel.isOpen) {
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
    if (this.currentChannel != null) {
      this.currentChannel.isConnected
    } else {
      this.connected
    }
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

        (m.name : @switch) match {
          case Message.BackendKeyData => {
            this._processData = Some(m.asInstanceOf[ProcessData])
          }
          case Message.BindComplete => {
          }
          case Message.Authentication => {
            this.onAuthenticationResponse(ctx.getChannel, m.asInstanceOf[AuthenticationMessage])
          }
          case Message.CommandComplete => {
            this.onCommandComplete(m.asInstanceOf[CommandCompleteMessage])
          }
          case Message.CloseComplete => {
          }
          case Message.DataRow => {
            this.onDataRow(m.asInstanceOf[DataRowMessage])
          }
          case Message.Error => {
            this.onError(m.asInstanceOf[ErrorMessage])
          }
          case Message.EmptyQueryString => {
            val exception = new QueryMustNotBeNullOrEmptyException(null)
            this.setErrorOnFutures(exception)
          }
          case Message.NoData => {
          }
          case Message.Notice => {
          }
          case Message.ParameterStatus => {
            this.onParameterStatus(m.asInstanceOf[ParameterStatusMessage])
          }
          case Message.ParseComplete => {
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
        log.error("[{}] - Unknown message type {}", this.currentCount, e.getMessage)
        throw new IllegalArgumentException("Unknown message type - %s".format(e.getMessage()))
      }

    }

  }

  override def sendQuery(query: String): Future[QueryResult] = {
    validateQuery(query)
    this.readyForQuery = false

    val promise = Promise[QueryResult]()
    this.setQueryPromise(promise)

    this.currentChannel.write(new QueryMessage(query))

    promise.future
  }

  override def sendPreparedStatement(query: String, values: Seq[Any] = List()): Future[QueryResult] = {
    validateQuery(query)

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

    if (paramsCount != values.length) {
      throw new InsufficientParametersException(paramsCount, values)
    }

    this.readyForQuery = false
    val promise = Promise[QueryResult]()
    this.setQueryPromise(promise)
    this.currentPreparedStatement = Some(realQuery)

    if (!this.isParsed(realQuery)) {
      this.currentChannel.write(new PreparedStatementOpeningMessage(realQuery, values, this.encoderRegistry))
    } else {
      this.currentQuery = Some(new MutableResultSet(this.parsedStatements.get(realQuery), configuration.charset, this.decoderRegistry))
      this.currentChannel.write(new PreparedStatementExecuteMessage(realQuery, values, this.encoderRegistry))
    }

    promise.future
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    this.setErrorOnFutures(e.getCause)
  }

  def hasRecentError: Boolean = this.recentError

  private def setErrorOnFutures(e: Throwable) {
    this.recentError = true

    log.error("[%s] - Error on connection".format(currentCount), e)

    if (!this.connectionFuture.isCompleted) {
      this.connectionFuture.failure(e)
      this.disconnect
    }

    this.failQueryPromise(e)

    this.currentPreparedStatement = None
  }

  override def channelDisconnected(ctx: ChannelHandlerContext, e: ChannelStateEvent): Unit = {
    log.info("[{}] - Connection disconnected - {}", this.currentCount, ctx.getChannel.getRemoteAddress)
    this.connected = false
  }

  private def onReadyForQuery {
    this.recentError = false
    this.readyForQuery = true
    this.clearQueryPromise

    if (!this.connectionFuture.isCompleted) {
      this.connectionFuture.success(this)
    }
  }

  private def onError(m: ErrorMessage) {
    log.error("[%s] - Error with message -> {}".format(currentCount), m)

    val error = new GenericDatabaseException(m)
    error.fillInStackTrace()

    this.setErrorOnFutures(error)
  }

  private def onCommandComplete(m: CommandCompleteMessage) {
    this.currentPreparedStatement = None
    this.succeedQueryPromise(new QueryResult(m.rowsAffected, m.statusMessage, this.currentQuery))
  }

  private def onParameterStatus(m: ParameterStatusMessage) {
    this.parameterStatus.put(m.key, m.value)
  }

  private def onDataRow(m: DataRowMessage) {
    this.currentQuery.get.addRawRow(m.values)
  }

  private def onRowDescription(m: RowDescriptionMessage) {
    this.currentQuery = Option(new MutableResultSet(m.columnDatas, configuration.charset, this.decoderRegistry))

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
        log.debug("[{}] - Successfully logged in to database", this.currentCount)
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

  private def validateQuery(query: String) {
    if (this.queryPromise.isDefined) {
      log.error("[{}] - Can't run query because there is one query pending already", this.currentCount)
      throw new ConnectionStillRunningQueryException(
        this.currentCount,
        this.readyForQuery
      )
    }

    if (query == null || query.isEmpty) {
      throw new QueryMustNotBeNullOrEmptyException(query)
    }
  }

  private def queryPromise: Option[Promise[QueryResult]] = queryPromiseReference.get()

  private def setQueryPromise(promise: Promise[QueryResult]) {
    this.queryPromiseReference.set(Some(promise))
  }

  private def clearQueryPromise {
    this.queryPromiseReference.set(None)
  }

  private def failQueryPromise(t: Throwable) {
    val promise = this.queryPromise

    if (promise.isDefined) {
      this.clearQueryPromise
      log.error("[{}] - Setting error on future {}", this.currentCount, promise)
      promise.get.failure(t)
    }
  }

  private def succeedQueryPromise(result: QueryResult) {
    val promise = this.queryPromise

    if (promise.isDefined) {
      this.clearQueryPromise
      promise.get.success(result)
    }
  }

  override def toString: String = {
    "%s{counter=%s}".format(this.getClass.getSimpleName, this.currentCount)
  }
}