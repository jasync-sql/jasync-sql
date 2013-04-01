package com.github.mauricio.postgresql

import org.jboss.netty.handler.codec.frame.FrameDecoder
import org.jboss.netty.buffer.ChannelBuffer
import com.github.mauricio.postgresql.parsers.{MessageParser, AuthenticationStartupParser}
import org.jboss.netty.channel.{ChannelHandlerContext, Channel}
import util.Log

object MessageDecoder extends FrameDecoder {

  val log = Log.getByName("MessageDecoder")

  override def decode(ctx: ChannelHandlerContext, c: Channel, b: ChannelBuffer) : Object = {

    if ( b.readableBytes() >= 5 ) {

      b.markReaderIndex()

      val code = b.readByte().asInstanceOf[Char]
      val lengthWithSelf = b.readInt()
      val length = lengthWithSelf - 4

      if ( b.readableBytes() >= length ) {
        code match {
          case 'R' => {
            AuthenticationStartupParser.parseMessage( b )
          }
          case _ => {
            MessageParser.parse( code, b.readSlice( length ) )
          }
        }

      } else {
        b.resetReaderIndex()
        return null
      }

    } else {
      return null
    }

  }

}