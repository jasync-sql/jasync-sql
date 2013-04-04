package com.github.mauricio.postgresql.parsers

import org.jboss.netty.buffer.ChannelBuffer
import com.github.mauricio.postgresql.messages.backend.Message

/**
 * User: mauricio
 * Date: 4/4/13
 * Time: 12:12 AM
 */
trait Decoder {

  def parseMessage(buffer: ChannelBuffer): Message

}
