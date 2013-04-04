package com.github.mauricio.postgresql.parsers

import org.jboss.netty.buffer.ChannelBuffer
import com.github.mauricio.postgresql.messages.backend.{ProcessData, Message}

/**
 * User: Maurício Linhares
 * Date: 2/28/12
 * Time: 11:13 PM
 */

object BackendKeyDataParser extends Decoder {

  override def parseMessage(b: ChannelBuffer): Message = {
    new ProcessData( b.readInt(), b.readInt() )
  }

}
