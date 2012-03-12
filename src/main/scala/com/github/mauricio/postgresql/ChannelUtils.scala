package com.github.mauricio.postgresql

import org.jboss.netty.buffer.ChannelBuffer
import util.Log

/**
 * User: Maurício Linhares
 * Date: 3/1/12
 * Time: 2:02 AM
 */

object ChannelUtils {

  private val log = Log.getByName("ChannelUtils")

  def writeLength( buffer : ChannelBuffer ) {

    val length = buffer.writerIndex() - 1
    buffer.markWriterIndex()
    buffer.writerIndex(1)
    buffer.writeInt( length )

    buffer.resetWriterIndex()

  }

  def printBuffer( b : ChannelBuffer ) : Unit = {

    val bytes = new Array[Byte](b.readableBytes())
    b.markReaderIndex()
    b.readBytes(bytes)
    b.resetReaderIndex()

    println(bytes.mkString("-"))

  }

  def writeCString( content : String, b : ChannelBuffer ) : Unit = {
    b.writeBytes( content.getBytes( CharsetHelper.Unicode ) )
    b.writeByte(0)
  }

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
