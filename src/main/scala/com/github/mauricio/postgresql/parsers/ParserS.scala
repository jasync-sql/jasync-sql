package com.github.mauricio.postgresql.parsers

import org.jboss.netty.buffer.ChannelBuffer
import com.github.mauricio.postgresql.{CharsetHelper, Message}

/**
 * User: Maurício Linhares
 * Date: 2/25/12
 * Time: 7:06 PM
 */

class ParserS extends MessageParser {

  override def parseMessage(b: ChannelBuffer): Message = {
    new Message( Message.ParameterStatus, ( readCString(b), readCString(b) ) )
  }

  private def readCString( b : ChannelBuffer ) : String = {

    b.markReaderIndex()

    var byte : Byte = 0
    var count = 0

    do {
      byte = b.readByte()
      count+= 1
    } while ( byte != 0 )

    b.resetReaderIndex()

    val result = b.toString( b.readerIndex(), count - 1, CharsetHelper.Unicode )

    b.readerIndex( b.readerIndex() + count)

    return result
  }

}
