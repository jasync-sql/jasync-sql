/*
 * Copyright 2013 Maurício Linhares
 *
 * Maurício Linhares licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.github.mauricio.async.db.util

import com.github.mauricio.async.db.exceptions.UnknownLengthException
import java.nio.charset.Charset
import org.jboss.netty.buffer.ChannelBuffer
import scala.language.implicitConversions

object ChannelWrapper {
  implicit def bufferToWrapper( buffer : ChannelBuffer ) = new ChannelWrapper(buffer)

  final val MySQL_NULL = 0xfb

}

class ChannelWrapper( val buffer : ChannelBuffer ) extends AnyVal {

  import ChannelWrapper.MySQL_NULL

  def readFixedString( length : Int, charset : Charset ) : String = {
    val bytes = new Array[Byte](length)
    buffer.readBytes( bytes )
    new String( bytes, charset )
  }

  def readCString( charset : Charset ) = ChannelUtils.readCString(buffer, charset)

  def readUntilEOF( charset: Charset ) = ChannelUtils.readUntilEOF(buffer, charset)

  def read3BytesInt : Int = {
    val first = buffer.readByte()
    val second = buffer.readByte()
    val third = buffer.readByte()
    var i = third << 16 | second  << 8 | first

    if ((third & 0x80) == 0x80) {
      i |= 0xff000000
    }

    i
  }

  def readLengthEncodedString( charset : Charset ) : String = {
    val length = readBinaryLength
    readFixedString(length.asInstanceOf[Int], charset)
  }

  def readBinaryLength : Long = {
    val firstByte = buffer.readUnsignedByte()

    if ( firstByte <= 250 ) {
      firstByte
    } else {
      firstByte match {
        case MySQL_NULL => -1
        case 252 => buffer.readUnsignedShort()
        case 253 => read3BytesInt
        case 254 => buffer.readLong()
        case _ => throw new UnknownLengthException(firstByte)
      }
    }

  }

  def writeLength( length : Long ) {
    if (length < 251) {
      buffer.writeByte( length.asInstanceOf[Byte])
    } else if (length < 65536L) {
      buffer.writeByte(252)
      buffer.writeShort(length.asInstanceOf[Int])
    } else if (length < 16777216L) {
      buffer.writeByte(253)
      writeLongInt(length.asInstanceOf[Int])
    } else {
      buffer.writeByte(254)
      buffer.writeLong(length)
    }
  }

  def writeLongInt(i : Int) {
    buffer.writeByte( i & 0xff )
    buffer.writeByte( i >>> 8 )
    buffer.writeByte( i >>> 16 )
  }

  def writeLenghtEncodedString( value : String, charset : Charset ) {
    val bytes = value.getBytes(charset)
    writeLength(bytes.length)
    buffer.writeBytes(bytes)
  }

  def writePacketLength( sequence : Int = 0 ) {
    ChannelUtils.writePacketLength(buffer, sequence )
  }

}
