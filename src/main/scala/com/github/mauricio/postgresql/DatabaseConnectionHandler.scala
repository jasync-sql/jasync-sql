package com.github.mauricio.postgresql

import parsers.ProcessData
import util.Log
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import java.util.concurrent.{Executors, ConcurrentHashMap}
import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel._
import java.net.InetSocketAddress
import scala.collection.JavaConversions._
import org.jboss.netty.buffer.ChannelBuffers

object DatabaseConnectionHandler {
  val log = Log.get[DatabaseConnectionHandler]
  val Name = "Netty-PostgreSQL-driver-0.0.1"
}

class DatabaseConnectionHandler( val host : String, val port : Int, val user: String, val database: String) extends SimpleChannelHandler {

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
  private var _processData : Option[ProcessData] = None

  private val factory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
  private val bootstrap = new ClientBootstrap(this.factory)
  private var channelFuture : ChannelFuture  = null

  def isReadyForQuery : Boolean = this.readyForQuery

  def connect : Unit = {

    val handler = this

    this.bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

      override def getPipeline(): ChannelPipeline = {
        return Channels.pipeline( new MessageDecoder(), handler )
      }

    });

    this.bootstrap.setOption("child.tcpNoDelay", true);
    this.bootstrap.setOption("child.keepAlive", true);
    this.channelFuture = this.bootstrap.connect(new InetSocketAddress( this.host, this.port)).awaitUninterruptibly()

  }

  def disconnect : Unit = {
    this.channelFuture.getChannel.getCloseFuture.awaitUninterruptibly()
  }

  def parameterStatuses : scala.collection.immutable.Map[String, String] = this.parameterStatus.toMap

  def processData : Option[ProcessData] = {
    _processData
  }

  override def channelConnected(ctx: ChannelHandlerContext, e: ChannelStateEvent): Unit = {

    val buffer = new OutputBuffer()

    buffer.writeInteger2(3)
    buffer.writeInteger2(0)

    properties.foreach {
      entry =>
        buffer.writeCString(entry._1)
        buffer.writeCString(entry._2)
    }

    buffer.writeByte(0)

    e.getChannel().write(buffer.toBuffer)
  }

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent): Unit = {

    e.getMessage() match {
      case m: Message => {

        log.debug( "name -> %s -> %s", m.name, m.content )

        m.name match {
          case Message.ParameterStatus => {
            val pair = m.content.asInstanceOf[(String, String)]
            this.parameterStatus.put( pair._1, pair._2  )
          }
          case Message.Error => {
            log.error("Error with message -> %s", m.content)
          }
          case Message.BackendKeyData => {
            this._processData = Option( m.content.asInstanceOf[ProcessData] )
          }
          case Message.ReadyForQuery => {
            log.debug("Connection ready for querying!")
            this.readyForQuery = true
          }
          case Message.RowDescription => {
            log.debug( "Row description is %s", m.content )
          }
          case Message.AuthenticationOk => {
            log.debug( "Authenticated to the database" )
          }
          case _  => {
            throw new IllegalStateException("Handler not implemented for message %s".format( m.name ))
          }
        }

      }
      case _ => {
        log.error( "Unknown message type %s -> %s", e.getMessage.getClass, e.getMessage )
        throw new IllegalArgumentException( "Unknown message type - %s".format( e.getMessage() ) )
      }

    }

  }

  def sendQuery( query : String ) : Unit = {

    val buffer = ChannelBuffers.dynamicBuffer()
    buffer.writeByte( Message.Query )
    val queryBytes = CharsetHelper.toBytes( query )
    buffer.writeInt( 5 + queryBytes.length )
    buffer.writeBytes(queryBytes)
    buffer.writeByte(0)

    this.channelFuture.getChannel.write( buffer )
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent): Unit = {
    e.getCause().printStackTrace()
    e.getChannel().close()
  }

}