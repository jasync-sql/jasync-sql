package com.github.mauricio.postgresql.parsers

import org.jboss.netty.buffer.ChannelBuffer
import com.github.mauricio.postgresql.messages.backend.{DataRowMessage, Message}

/**
 * User: Maurício Linhares
 * Date: 3/4/12
 * Time: 10:56 AM
 */

object DataRowParser extends Decoder {

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

    new DataRowMessage(row)
  }

}
