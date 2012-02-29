package com.github.mauricio.postgresql

import org.jboss.netty.channel.SimpleChannelHandler
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.ExceptionEvent
import org.jboss.netty.channel.ChannelStateEvent
import org.jboss.netty.channel.MessageEvent
import java.util.concurrent.ConcurrentHashMap
import parsers.ProcessData
import util.Log

object DatabaseConnectionHandler {
  val log = Log.get[DatabaseConnectionHandler]
}

class DatabaseConnectionHandler(val user: String, val database: String) extends SimpleChannelHandler {

  private val log = DatabaseConnectionHandler.log

  private val properties = List(
    "user" -> user,
    "database" -> database,
    "application_name" -> Connection.Name,
    "client_encoding" -> "UTF8",
    "DateStyle" -> "ISO",
    "extra_float_digits" -> "2")

  val parameterStatus = new ConcurrentHashMap[String, String]()
  private var _processData : Option[ProcessData] = None

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

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent): Unit = {
    e.getCause().printStackTrace()
    e.getChannel().close()
  }

}