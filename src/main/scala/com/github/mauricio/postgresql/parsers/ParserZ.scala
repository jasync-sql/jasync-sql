package com.github.mauricio.postgresql.parsers

import com.github.mauricio.postgresql.{Message}
import org.jboss.netty.buffer.ChannelBuffer

/**
 * User: Maurício Linhares
 * Date: 2/29/12
 * Time: 12:33 AM
 */

object ParserZ extends MessageParser {

  override def parseMessage(b: ChannelBuffer): Message = {
    new Message( Message.ReadyForQuery , b.readByte().asInstanceOf[Char])
  }

}
