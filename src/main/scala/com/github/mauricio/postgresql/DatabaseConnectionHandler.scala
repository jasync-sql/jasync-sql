package com.github.mauricio.postgresql

import messages.{CloseMessage, QueryMessage, StartupMessage}
import parsers.ProcessData
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel._
import java.net.InetSocketAddress
import scala.collection.JavaConversions._
import java.util.concurrent.{Future, Executors, ConcurrentHashMap}
import util.{DaemonThreadsFactory, BasicFuture, Log}

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
    Executors.newCachedThreadPool( DaemonThreadsFactory ),
    Executors.newCachedThreadPool( DaemonThreadsFactory ))

  private val bootstrap = new ClientBootstrap(this.factory)
  private var channelFuture : ChannelFuture  = null
  private var connectionFuture : Option[BasicFuture[Map[String, String]]] = None
  private var queryFuture : Option[BasicFuture[QueryResult]] = None

  def isReadyForQuery : Boolean = this.readyForQuery

  def connect : Future[Map[String,String]] = {

    this.bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

      override def getPipeline(): ChannelPipeline = {
        return Channels.pipeline( MessageDecoder, MessageEncoder, DatabaseConnectionHandler.this )
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
            log.debug( "Command was run correctly -> {}", m.content )

            if ( this.queryFuture.isDefined ) {
              this.queryFuture.get.set( m.content.asInstanceOf[QueryResult] )
              this.queryFuture = None
            }

          }
          case Message.Error => {
            log.error("Error with message -> {}", m.content)

            val error = new IllegalStateException( m.content.toString )
            error.fillInStackTrace()

            this.setErrorOnFutures(error)

          }
          case Message.Notice => {
            log.debug( "notice -> {}", m.content.asInstanceOf[List[(Char,String)]].mkString(" ") )
          }
          case Message.ParameterStatus => {
            val pair = m.content.asInstanceOf[(String, String)]
            log.debug( "Parameter - ({}->{})", pair._1, pair._2 )
            this.parameterStatus.put( pair._1, pair._2  )
          }
          case Message.ReadyForQuery => {
            log.debug("Connection ready for querying!")

            this.readyForQuery = true

            if ( this.connectionFuture.isDefined ) {
              this.connectionFuture.get.set( this.parameterStatus.toMap )
              this.connectionFuture = None
            }

          }
          case Message.RowDescription => {
            log.debug( "Row description {} %s", m.content )
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

}