package com.github.mauricio.postgresql.parsers

import org.jboss.netty.buffer.ChannelBuffer
import com.github.mauricio.postgresql.Message

/**
 * User: Maurício Linhares
 * Date: 2/28/12
 * Time: 11:13 PM
 */

object ParserK extends MessageParser {

  override def parseMessage(b: ChannelBuffer): Message = {
    new Message( Message.BackendKeyData, new ProcessData( b.readInt(), b.readInt() ) )
  }

}
