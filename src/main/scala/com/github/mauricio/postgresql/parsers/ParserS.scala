package com.github.mauricio.postgresql.parsers

import org.jboss.netty.buffer.ChannelBuffer
import com.github.mauricio.postgresql.{ChannelUtils, Message}

/**
 * User: Maurício Linhares
 * Date: 2/25/12
 * Time: 7:06 PM
 */

class ParserS extends MessageParser {

  import ChannelUtils._

  override def parseMessage(b: ChannelBuffer): Message = {
    new Message( Message.ParameterStatus, ( readCString(b), readCString(b) ) )
  }

}
