package com.github.mauricio.postgresql

import org.jboss.netty.buffer.ChannelBuffer
import util.Log
import org.jboss.netty.util.CharsetUtil
import java.nio.charset.Charset

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

  def writeCString( content : String, b : ChannelBuffer, charset : Charset ) : Unit = {
    b.writeBytes( content.getBytes( charset ) )
    b.writeByte(0)
  }

  def readCString( b : ChannelBuffer, charset : Charset ) : String = {

    b.markReaderIndex()

    var byte : Byte = 0
    var count = 0

    do {
      byte = b.readByte()
      count+= 1
    } while ( byte != 0 )

    b.resetReaderIndex()

    val result = b.toString( b.readerIndex(), count - 1, charset )

    b.readerIndex( b.readerIndex() + count)

    return result
  }

}
