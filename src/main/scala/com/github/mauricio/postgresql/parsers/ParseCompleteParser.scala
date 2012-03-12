package com.github.mauricio.postgresql.parsers

import org.jboss.netty.buffer.ChannelBuffer
import com.github.mauricio.postgresql.Message

/**
 * User: Maurício Linhares
 * Date: 3/12/12
 * Time: 2:28 AM
 */

object ParseCompleteParser extends MessageParser {
  def parseMessage(buffer: ChannelBuffer): Message = {
    ParseComplete.Instance
  }
}
