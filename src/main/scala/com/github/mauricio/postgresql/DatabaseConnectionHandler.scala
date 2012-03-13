package com.github.mauricio.postgresql

import messages._
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel._
import java.net.InetSocketAddress
import parsers.{ColumnData, ProcessData}
import scala.collection.JavaConversions._
import org.jboss.netty.buffer.ChannelBuffer
import util.{Future, SimpleFuture, Log}
import java.util.concurrent.ConcurrentHashMap
import org.jboss.netty.logging.{Slf4JLoggerFactory, InternalLoggerFactory}

object DatabaseConnectionHandler {
  val log = Log.get[DatabaseConnectionHandler]
  val Name = "Netty-PostgreSQL-driver-0.0.1"
  InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory())
}

class DatabaseConnectionHandler
    (
      val host : String,
      val port : Int,
      val user: String,
      val database: String) extends SimpleChannelHandler with Connection {

  import DatabaseConnectionHandler._

  private val properties = List(
    "user" -> user,
    "database" -> database,
    "application_name" -> DatabaseConnectionHandler.Name,
    "client_encoding" -> "UTF8",
    "DateStyle" -> "ISO",
    "extra_float_digits" -> "2")

  private var readyForQuery = false
  private val parameterStatus = new ConcurrentHashMap[String, String]()
  private val parsedStatements = new ConcurrentHashMap[String,Array[ColumnData]]()
  private var _processData : Option[ProcessData] = None

  private val factory = new NioClientSocketChannelFactory(
    ExecutorServiceUtils.CachedThreadPool,
    ExecutorServiceUtils.CachedThreadPool)

  private val bootstrap = new ClientBootstrap(this.factory)
  private var channelFuture : ChannelFuture  = null

  @volatile private var connected = false
  @volatile private var connectionFuture : Option[SimpleFuture[Map[String, String]]] = None
  @volatile private var queryFuture : Option[SimpleFuture[QueryResult]] = None
  @volatile private var currentQuery : Option[Query] = None
  @volatile private var currentPreparedStatement : Option[String] = None

  def isReadyForQuery : Boolean = this.readyForQuery

  def connect : Future[Map[String,String]] = {

    this.bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

      override def getPipeline(): ChannelPipeline = {
        Channels.pipeline( MessageDecoder, MessageEncoder, DatabaseConnectionHandler.this )
      }

    });

    this.bootstrap.setOption("child.tcpNoDelay", true)
    this.bootstrap.setOption("child.keepAlive", true)

    this.connectionFuture = Some( new SimpleFuture[Map[String, String]]() )

    this.channelFuture = this.bootstrap.connect(new InetSocketAddress( this.host, this.port)).awaitUninterruptibly()

    this.connectionFuture.get
  }

  override def disconnect : Unit = {

    if ( this.connected ) {
      this.channelFuture.getChannel.write( CloseMessage.Instance )
    }

  }

  override def isConnected : Boolean = {
    this.connected
  }

  def parameterStatuses : scala.collection.immutable.Map[String, String] = this.parameterStatus.toMap

  def processData : Option[ProcessData] = {
    _processData
  }

  override def channelConnected(ctx: ChannelHandlerContext, e: ChannelStateEvent): Unit = {
    this.connected = true
    e.getChannel().write( new StartupMessage( this.properties ) )
  }

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent): Unit = {

    e.getMessage() match {
      case m: Message => {

        m.name match {
          case Message.AuthenticationOk => {
            log.info( "Authenticated to the database" )
          }
          case Message.BackendKeyData => {
            this._processData = Option( m.content.asInstanceOf[ProcessData] )
          }
          case Message.BindComplete => {
            log.debug("Finished binding statement")
          }
          case Message.CommandComplete => {
            this.onCommandComplete(m)
          }
          case Message.CloseComplete => {
            log.debug("Successfully closed portal for [{}]", this.currentPreparedStatement)
          }
          case Message.DataRow => {
            this.onDataRow(m)
          }
          case Message.Error => {
            this.onError(m)
          }
          case Message.Notice => {
            log.info( "notice -> {}", m.content.asInstanceOf[List[(Char,String)]].mkString(" ") )
          }
          case Message.ParameterStatus => {
            this.onParameterStatus(m)
          }
          case Message.ParseComplete => {
            log.debug("Finished parsing statement")
          }
          case Message.ReadyForQuery => {
            this.onReadyForQuery
          }
          case Message.RowDescription => {
            this.onRowDescription( m.content.asInstanceOf[Array[ColumnData]] )
          }
          case _  => {
            throw new IllegalStateException("Handler not implemented for message %s".format( m.name ))
          }
        }

      }
      case _ => {
        log.error( "Unknown message type {} -> {}", e.getMessage.getClass, e.getMessage )
        throw new IllegalArgumentException( "Unknown message type - %s".format( e.getMessage() ) )
      }

    }

  }

  override def sendQuery( query : String ) : Future[QueryResult] = {
    this.readyForQuery = false
    this.queryFuture = Option(new SimpleFuture[QueryResult]())
    this.channelFuture.getChannel.write( new QueryMessage( query ) )
    this.queryFuture.get
  }

  def sendPreparedStatement( query : String, values : Any* ) : Future[QueryResult] = {
    this.readyForQuery = false
    this.queryFuture = Some(new SimpleFuture[QueryResult]())

    var paramsCount = 0

    val realQuery = if ( query.contains("?") ) {
      query.foldLeft( new StringBuilder() ) {
        (builder, char) =>
          if ( char == '?' ) {
            paramsCount += 1
            builder.append("$" + paramsCount)
          } else {
            builder.append( char )
          }
          builder
      }.toString()
    } else {
      query
    }

    this.currentPreparedStatement = Some(realQuery)

    if ( !this.isParsed(realQuery) ) {
      log.debug("Query is not parsed yet -> {}",realQuery)
      this.channelFuture.getChannel.write( new PreparedStatementOpeningMessage( realQuery, values  ) )
    } else {
      this.currentQuery = Some( new Query(this.parsedStatements.get(realQuery)) )
      this.channelFuture.getChannel.write( new PreparedStatementExecuteMessage( realQuery, values ) )
    }

    this.queryFuture.get
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    this.setErrorOnFutures(e.getCause)
  }

  private def setErrorOnFutures( e : Throwable ) {

    log.error( "Error on connection", e )

    if ( this.connectionFuture.isDefined ) {
      this.connectionFuture.get.setError(e)
      this.connectionFuture = None
    }

    if ( this.queryFuture.isDefined ) {
      log.error( "Setting error on future {}", this.queryFuture.get )
      this.queryFuture.get.setError( e.getCause )
      this.queryFuture = None
      this.currentPreparedStatement = None
    }

  }

  override def channelDisconnected( ctx : ChannelHandlerContext, e : ChannelStateEvent ) : Unit = {
    log.warn( "Connection disconnected - {} - {}", e.getState, e.getValue )
    this.connected = false
  }

  private def onReadyForQuery {

    this.readyForQuery = true

    if ( this.connectionFuture.isDefined ) {
      this.connectionFuture.get.set( this.parameterStatus.toMap )
      this.connectionFuture = None
    }
  }

  private def onError( m : Message ) {
    log.error("Error with message -> {}", m.content)

    val error = new IllegalStateException( m.content.toString )
    error.fillInStackTrace()

    this.setErrorOnFutures(error)
  }

  private def onCommandComplete(m : Message) {

    if ( this.queryFuture.isDefined ) {

      val result = m.content.asInstanceOf[(Int,String)]

      val queryResult = if ( this.currentQuery.isDefined ) {
        new QueryResult( result._1, result._2, Some(this.currentQuery.get) )
      } else {
        new QueryResult( result._1, result._2, None )
      }

      this.queryFuture.get.set(queryResult)
      this.queryFuture = None
      this.currentPreparedStatement = None

    }
  }

  private def onParameterStatus(m : Message) {
    val pair = m.content.asInstanceOf[(String, String)]
    this.parameterStatus.put( pair._1, pair._2  )
  }

  private def onDataRow(m : Message) {
    this.currentQuery.get.addRawRow(m.content.asInstanceOf[Array[ChannelBuffer]])
  }

  private def onRowDescription( values : Array[ColumnData] ) {
    log.debug("received query description")
    this.currentQuery = Option( new Query( values ) )

    log.debug("Current prepared statement is {}", this.currentPreparedStatement)

    if ( this.currentPreparedStatement.isDefined ) {
      this.parsedStatements.put(this.currentPreparedStatement.get, values)
      log.debug("parsed statements are -> {}", this.parsedStatements)
    }
  }

  private def isParsed( query : String ) : Boolean = {
    this.parsedStatements.containsKey(query)
  }

}