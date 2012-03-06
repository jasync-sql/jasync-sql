package com.github.mauricio.postgresql.parsers

import com.github.mauricio.postgresql.Message
import org.jboss.netty.buffer.ChannelBuffer

/**
 * User: Maurício Linhares
 * Date: 3/4/12
 * Time: 10:56 AM
 */

object DataRowParser extends MessageParser {

  def parseMessage(buffer: ChannelBuffer): Message = {

    val row = new Array[ChannelBuffer](buffer.readShort())

    0.until( row.length ).foreach {
      column =>
        val length = buffer.readInt()

        row(column) = if (length == -1) {
          null
        } else {
          val slice = buffer.slice( buffer.readerIndex(), length )
          buffer.readerIndex( buffer.readerIndex() + length )
          slice
        }
    }

    new Message(Message.DataRow, row)
  }

}
