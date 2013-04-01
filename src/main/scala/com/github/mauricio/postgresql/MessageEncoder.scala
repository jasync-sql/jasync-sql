package com.github.mauricio.postgresql

import encoders._
import exceptions.EncoderNotAvailableException
import messages.frontend._
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

  val encoders : Map[Class[_],Encoder] = Map(
    classOf[CloseMessage] -> CloseMessageEncoder,
    classOf[PreparedStatementExecuteMessage] -> ExecutePreparedStatementEncoder,
    classOf[PreparedStatementOpeningMessage] -> PreparedStatementOpeningEncoder,
    classOf[StartupMessage] -> StartupMessageEncoder,
    classOf[QueryMessage] -> QueryMessageEncoder,
    classOf[CredentialMessage] -> CredentialEncoder
  )

  override def encode(ctx: ChannelHandlerContext, channel: Channel, msg: AnyRef): ChannelBuffer = {

    val buffer = msg match {
      case message : FrontendMessage => {
        val option = this.encoders.get( message.getClass )
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
