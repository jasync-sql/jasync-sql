package com.github.mauricio.postgresql

import messages.backend.Message
import org.jboss.netty.handler.codec.frame.FrameDecoder
import org.jboss.netty.buffer.ChannelBuffer
import com.github.mauricio.postgresql.parsers.{MessageParser, AuthenticationStartupParser}
import org.jboss.netty.channel.{ChannelHandlerContext, Channel}
import util.Log
import java.nio.charset.Charset

object MessageDecoder {
  val log = Log.getByName("MessageDecoder")
}

class MessageDecoder ( charset : Charset ) extends FrameDecoder {

  private val parser = new MessageParser(charset)

  override def decode(ctx: ChannelHandlerContext, c: Channel, b: ChannelBuffer) : Object = {

    if ( b.readableBytes() >= 5 ) {

      b.markReaderIndex()

      val code = b.readByte().asInstanceOf[Char]
      val lengthWithSelf = b.readInt()
      val length = lengthWithSelf - 4

      if ( b.readableBytes() >= length ) {
        code match {
          case Message.Authentication => {
            AuthenticationStartupParser.parseMessage( b )
          }
          case _ => {
            parser.parse( code, b.readSlice( length ) )
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