package com.github.mauricio.postgresql

import org.jboss.netty.handler.codec.frame.FrameDecoder
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.Channel
import com.github.mauricio.postgresql.parsers.{MessageParser, ParserR}

class MessageDecoder extends FrameDecoder {
  
  override def decode(ctx: ChannelHandlerContext, c: Channel, b: ChannelBuffer) : Object = {

    var code : Char = 0
    var length : Int = 0

    if ( b.readableBytes() >= 5 ) {

      b.markReaderIndex()

      code = b.readByte().asInstanceOf[Char]
      length = b.readInt() - 4

      if ( b.readableBytes() >= length ) {

        code match {
          case 'R' => {
            ParserR.Instance.parseMessage( b )
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