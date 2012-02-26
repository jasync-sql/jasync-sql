package com.github.mauricio.postgresql

import org.jboss.netty.channel.SimpleChannelHandler
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.ExceptionEvent
import org.jboss.netty.channel.ChannelStateEvent
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.buffer.ChannelBuffer
import java.util.concurrent.{ConcurrentHashMap}
import util.Log

object DatabaseConnectionHandler {
  val log = Log.get[DatabaseConnectionHandler]
}

class DatabaseConnectionHandler(val user: String, val database: String) extends SimpleChannelHandler {

  private val log = DatabaseConnectionHandler.log

  val properties = List(
    "user" -> user,
    "database" -> database,
    "application_name" -> Connection.Name,
    "client_encoding" -> "UTF8",
    "DateStyle" -> "ISO",
    "extra_float_digits" -> "2")

  val parameterStatus = new ConcurrentHashMap[String, String]()

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
          case _  => {
            throw new IllegalStateException("Handler not implemented for message %s".format( m.name ))
          }
        }

      }
      case buffer: ChannelBuffer => {
        if (buffer.readableBytes() > 0) {
          val result = new Array[Byte](buffer.readableBytes())
          buffer.readBytes(result)
          log.debug( "message result is => %s", new String(result) )
        }
      }
      case _ => {
        throw new IllegalArgumentException( "Unknown message type - %s".format( e.getMessage() ) )
      }

    }

  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent): Unit = {
    e.getCause().printStackTrace()
    e.getChannel().close()
  }

}