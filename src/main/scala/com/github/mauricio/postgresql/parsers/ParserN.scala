package com.github.mauricio.postgresql.parsers

import org.jboss.netty.buffer.ChannelBuffer
import collection.mutable.ListBuffer
import com.github.mauricio.postgresql.{ChannelUtils, Message}

/**
 * User: Maurício Linhares
 * Date: 3/1/12
 * Time: 10:06 PM
 */

object ParserN extends MessageParser {

  override def parseMessage(b: ChannelBuffer): Message = {

    val listBuffer = ListBuffer[(Char, String)]()

    while ( b.readable() ) {
      val kind = b.readByte()

      if ( kind != 0 ) {
        listBuffer.append( ( kind.asInstanceOf[Char], ChannelUtils.readCString(b) ) )
      }

    }

    new Message( Message.Notice, listBuffer.toList )

  }

}
