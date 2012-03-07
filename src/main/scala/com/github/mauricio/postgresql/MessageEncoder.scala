package com.github.mauricio.postgresql

import encoders.{ParseMessageEncoder, CloseMessageEncoder, QueryMessageEncoder, StartupMessageEncoder}
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder
import org.jboss.netty.channel.{Channel, ChannelHandlerContext}

/**
 * User: Maurício Linhares
 * Date: 3/3/12
 * Time: 7:14 PM
 */

object MessageEncoder extends OneToOneEncoder {

  val encoders = Map(
    Message.Query -> QueryMessageEncoder,
    Message.Close -> CloseMessageEncoder,
    Message.Parse -> ParseMessageEncoder,
    Message.Startup -> StartupMessageEncoder
  )

  def encode(ctx: ChannelHandlerContext, channel: Channel, msg: AnyRef): AnyRef = {

    msg match {
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

  }

}
