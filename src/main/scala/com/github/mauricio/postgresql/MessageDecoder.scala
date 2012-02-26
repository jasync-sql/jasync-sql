package com.github.mauricio.postgresql

import org.jboss.netty.handler.codec.frame.FrameDecoder
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.Channel
import org.jboss.netty.buffer.ChannelBuffers
import parsers.ParserR

class MessageDecoder extends FrameDecoder {
  
  protected def decode(ctx: ChannelHandlerContext, c: Channel, b: ChannelBuffer): Object = { 

    var code : Char = 0
    var length : Int = 0

    if ( b.readableBytes() >= 5 ) {

      b.markReaderIndex()

      code = b.readByte().asInstanceOf[Char]
      length = b.readInt() - 4

      code match {
        case 'R' => {
          ParserR.Instance.parseMessage( b )
        }
        case _ => {

          if ( b.readableBytes() >= length ) {
            MessageParser.parse( code, b.readSlice( length ) )
          } else {
            return null
          }

        }
      }


    } else {
      return null
    }

  }

}