package com.github.mauricio.postgresql.parsers

import org.jboss.netty.buffer.ChannelBuffer
import com.github.mauricio.postgresql.Message
import java.nio.charset.Charset

object ErrorParser extends MessageParser {

  override def parseMessage(b: ChannelBuffer): Message = {
    new Message( Message.Error , b.toString( Charset.forName("UTF-8") ))
  }

}