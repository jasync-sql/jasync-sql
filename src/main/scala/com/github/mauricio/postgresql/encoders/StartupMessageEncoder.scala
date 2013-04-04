package com.github.mauricio.postgresql.encoders

import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}
import com.github.mauricio.postgresql.ChannelUtils
import com.github.mauricio.postgresql.messages.frontend.{FrontendMessage, StartupMessage}
import java.nio.charset.Charset


/**
 * User: Maurício Linhares
 * Date: 3/3/12
 * Time: 7:35 PM
 */

class StartupMessageEncoder (charset : Charset) extends Encoder {

  //private val log = Log.getByName("StartupMessageEncoder")

  override def encode(message: FrontendMessage): ChannelBuffer = {

    val startup = message.asInstanceOf[StartupMessage]

    val buffer = ChannelBuffers.dynamicBuffer()
    buffer.writeInt(0)
    buffer.writeShort( 3 )
    buffer.writeShort( 0 )

    startup.parameters.foreach {
      pair =>
        pair._2 match {
          case value : String => {
            ChannelUtils.writeCString( pair._1, buffer, charset )
            ChannelUtils.writeCString( value, buffer, charset )
          }
          case Some(value) => {
            ChannelUtils.writeCString( pair._1, buffer, charset )
            ChannelUtils.writeCString( value.toString, buffer, charset )
          }
          case _ => {}
        }
    }

    buffer.writeByte(0)

    val index = buffer.writerIndex()

    buffer.markWriterIndex()
    buffer.writerIndex(0)
    buffer.writeInt(index)
    buffer.resetWriterIndex()

    buffer
  }

}
