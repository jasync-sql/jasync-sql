package com.github.mauricio.postgresql

import encoders.{PreparedStatementOpeningEncoder, CloseMessageEncoder, QueryMessageEncoder, StartupMessageEncoder}
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder
import org.jboss.netty.channel.{Channel, ChannelHandlerContext}
import org.jboss.netty.buffer.ChannelBuffer
import util.Log

/**
 * User: Maurício Linhares
 * Date: 3/3/12
 * Time: 7:14 PM
 */

object MessageEncoder extends OneToOneEncoder {

  val log = Log.getByName("MessageEncoder")

  val encoders = Map(
    Message.Query -> QueryMessageEncoder,
    Message.Close -> CloseMessageEncoder,
    Message.Parse -> PreparedStatementOpeningEncoder,
    Message.Startup -> StartupMessageEncoder
  )

  override def encode(ctx: ChannelHandlerContext, channel: Channel, msg: AnyRef): ChannelBuffer = {

    val buffer = msg match {
      case message : FrontendMessage => {
        val option = this.encoders.get( message.kind )
        if ( option.isDefined ) {
          option.get.encode(message)
        } else {
          throw new EncoderNotAvailableException( message )
        }
      }
      case _ => {
        throw new IllegalArgumentException( "Can not encode message %s".format( msg ) )
      }
    }

    buffer

  }

}
