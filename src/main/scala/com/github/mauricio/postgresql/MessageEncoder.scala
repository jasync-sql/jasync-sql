package com.github.mauricio.postgresql

import encoders.{CloseMessageEncoder, QueryMessageEncoder, StartupMessageEncoder}
import messages.StartupMessage
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
    Message.Close -> CloseMessageEncoder
  )

  def encode(ctx: ChannelHandlerContext, channel: Channel, msg: AnyRef): AnyRef = {

    msg match {
      case message : StartupMessage => {
        StartupMessageEncoder.encode(message)
      }
      case message : Message => {
        val option = this.encoders.get( message.name )
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
