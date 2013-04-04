package com.github.mauricio.postgresql

import encoders._
import exceptions.EncoderNotAvailableException
import messages.frontend._
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder
import org.jboss.netty.channel.{Channel, ChannelHandlerContext}
import org.jboss.netty.buffer.ChannelBuffer
import util.Log
import java.nio.charset.Charset

/**
 * User: Maurício Linhares
 * Date: 3/3/12
 * Time: 7:14 PM
 */

class MessageEncoder( charset : Charset ) extends OneToOneEncoder {

  val log = Log.getByName("MessageEncoder")

  val encoders : Map[Class[_],Encoder] = Map(
    classOf[CloseMessage] -> CloseMessageEncoder,
    classOf[PreparedStatementExecuteMessage] -> new ExecutePreparedStatementEncoder( charset ),
    classOf[PreparedStatementOpeningMessage] -> new PreparedStatementOpeningEncoder( charset ),
    classOf[StartupMessage] -> new StartupMessageEncoder(charset),
    classOf[QueryMessage] -> new QueryMessageEncoder(charset),
    classOf[CredentialMessage] -> new CredentialEncoder( charset )
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
