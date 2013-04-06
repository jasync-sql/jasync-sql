package com.github.mauricio.postgresql.parsers

import com.github.mauricio.postgresql.messages.backend.Message
import org.jboss.netty.buffer.ChannelBuffer

/**
 * User: Maurício Linhares
 * Date: 3/12/12
 * Time: 11:36 PM
 */

class ReturningMessageParser( val message : Message ) extends Decoder {
  def parseMessage(buffer: ChannelBuffer): Message = {
    this.message
  }
}
