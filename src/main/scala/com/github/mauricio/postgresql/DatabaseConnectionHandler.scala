package com.github.mauricio.postgresql

import messages.{CloseMessage, QueryMessage, StartupMessage}
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel._
import java.net.InetSocketAddress
import parsers.{ColumnData, ProcessData}
import scala.collection.JavaConversions._
import java.util.concurrent.{Future, Executors, ConcurrentHashMap}
import util.{DaemonThreadsFactory, BasicFuture, Log}
import org.jboss.netty.buffer.ChannelBuffer

object DatabaseConnectionHandler {
  val log = Log.get[DatabaseConnectionHandler]
  val Name = "Netty-PostgreSQL-driver-0.0.1"
}

class DatabaseConnectionHandler
    (
      val host : String,
      val port : Int,
      val user: String,
      val database: String) extends SimpleChannelHandler {

  import DatabaseConnectionHandler._

  @volatile private var connected = false

  private val properties = Map(
    "user" -> user,
    "database" -> database,
    "application_name" -> DatabaseConnectionHandler.Name,
    "client_encoding" -> "UTF8",
    "DateStyle" -> "ISO",
    "extra_float_digits" -> "2")

  private var readyForQuery = false
  private val parameterStatus = new ConcurrentHashMap[String, String]()
  private var _processData : Option[ProcessData] = None

  private val factory = new NioClientSocketChannelFactory(
    ExecutorServiceUtils.CachedThreadPool,
    ExecutorServiceUtils.CachedThreadPool)

  private val bootstrap = new ClientBootstrap(this.factory)
  private var channelFuture : ChannelFuture  = null
  @volatile private var connectionFuture : Option[BasicFuture[Map[String, String]]] = None
  @volatile private var queryFuture : Option[BasicFuture[QueryResult]] = None
  @volatile private var currentQuery : Option[Query] = None

  def isReadyForQuery : Boolean = this.readyForQuery

  def connect : Future[Map[String,String]] = {

    this.bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

      override def getPipeline(): ChannelPipeline = {
        Channels.pipeline( MessageDecoder, MessageEncoder, DatabaseConnectionHandler.this )
      }

    });

    this.bootstrap.setOption("child.tcpNoDelay", true)
    this.bootstrap.setOption("child.keepAlive", true)

    this.connectionFuture = Some( new BasicFuture[Map[String, String]]() )

    this.channelFuture = this.bootstrap.connect(new InetSocketAddress( this.host, this.port)).awaitUninterruptibly()

    this.connectionFuture.get
  }

  def disconnect : Unit = {

    if ( this.connected ) {
      this.channelFuture.getChannel.write( CloseMessage.Instance )
    }

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
            log.debug( "Authenticated to the database" )
          }
          case Message.BackendKeyData => {
            this._processData = Option( m.content.asInstanceOf[ProcessData] )
          }
          case Message.CommandComplete => {
            this.onCommandComplete(m)
          }
          case Message.DataRow => {
            this.onDataRow(m)
          }
          case Message.Error => {
            this.onError(m)
          }
          case Message.Notice => {
            log.debug( "notice -> {}", m.content.asInstanceOf[List[(Char,String)]].mkString(" ") )
          }
          case Message.ParameterStatus => {
            this.onParameterStatus(m)
          }
          case Message.ReadyForQuery => {
            this.onReadyForQuery
          }
          case Message.RowDescription => {
            this.currentQuery = Option( new Query(m.content.asInstanceOf[Array[ColumnData]]) )
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

  def sendQuery( query : String ) : Future[QueryResult] = {
    this.queryFuture = Option(new BasicFuture[QueryResult]())
    this.channelFuture.getChannel.write( new QueryMessage( query ) )
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
      this.queryFuture.get.setError( e.getCause )
      this.queryFuture = None
    }

  }

  override def channelDisconnected( ctx : ChannelHandlerContext, e : ChannelStateEvent ) : Unit = {
    log.debug( "Connection disconnected" )
  }

  private def onReadyForQuery {
    log.debug("Connection ready for querying!")

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
    log.debug( "Command was run correctly -> {}", m.content )

    if ( this.queryFuture.isDefined ) {

      val result = m.content.asInstanceOf[(Int,String)]

      val queryResult = if ( this.currentQuery.isDefined ) {
        new QueryResult( result._1, result._2, Some(this.currentQuery.get) )
      } else {
        new QueryResult( result._1, result._2, None )
      }

      this.queryFuture.get.set(queryResult)
      this.queryFuture = None
    }
  }

  private def onParameterStatus(m : Message) {
    val pair = m.content.asInstanceOf[(String, String)]
    this.parameterStatus.put( pair._1, pair._2  )
  }

  private def onDataRow(m : Message) {
    this.currentQuery.get.addRawRow(m.content.asInstanceOf[Array[ChannelBuffer]])
  }

}