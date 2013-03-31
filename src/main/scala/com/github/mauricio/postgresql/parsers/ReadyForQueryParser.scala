package com.github.mauricio.postgresql.parsers

import org.jboss.netty.buffer.ChannelBuffer
import com.github.mauricio.postgresql.messages.backend.{ReadyForQueryMessage, Message}

/**
 * User: Maurício Linhares
 * Date: 2/29/12
 * Time: 12:33 AM
 */

object ReadyForQueryParser extends MessageParser {

  override def parseMessage(b: ChannelBuffer): Message = {
    new ReadyForQueryMessage( b.readByte().asInstanceOf[Char] )
  }

}
