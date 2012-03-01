package com.github.mauricio.postgresql

import org.jboss.netty.buffer.ChannelBuffer

/**
 * User: Maurício Linhares
 * Date: 3/1/12
 * Time: 2:02 AM
 */

object ChannelUtils {

  def readCString( b : ChannelBuffer ) : String = {

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
