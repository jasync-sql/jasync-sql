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

package com.github.mauricio.async.db.postgresql.column

import com.github.mauricio.async.db.column.ColumnEncoderDecoder
import com.github.mauricio.async.db.postgresql.exceptions.ByteArrayFormatNotSupportedException
import com.github.mauricio.async.db.util.{ Log, HexCodec }
import java.nio.ByteBuffer

import io.netty.buffer.ByteBuf

object ByteArrayEncoderDecoder extends ColumnEncoderDecoder {

  final val log = Log.getByName(this.getClass.getName)
  final val HexStart = "\\x"
  final val HexStartChars = HexStart.toCharArray

  override def decode(value: String): Array[Byte] = {

    if (value.startsWith(HexStart)) {
      HexCodec.decode(value, 2)
    } else {
      // Default encoding is 'escape'

      // Size the buffer to the length of the string, the data can't be bigger
      val buffer = ByteBuffer.allocate(value.length)

      val ci = value.iterator

      while (ci.hasNext) {
        ci.next match {
          case '\\' ⇒ getCharOrDie(ci) match {
            case '\\' ⇒ buffer.put('\\'.toByte)
            case firstDigit ⇒
              val secondDigit = getCharOrDie(ci)
              val thirdDigit = getCharOrDie(ci)
              // Must always be in triplets
              buffer.put(
                Integer.decode(
                  new String(Array('0', firstDigit, secondDigit, thirdDigit))).toByte)
          }
          case c ⇒ buffer.put(c.toByte)
        }
      }

      buffer.flip
      val finalArray = new Array[Byte](buffer.remaining())
      buffer.get(finalArray)

      finalArray
    }

  }

  /**
   * This is required since {@link Iterator#next} when {@linke Iterator#hasNext} is false is undefined.
   * @param ci the iterator source of the data
   * @return the next character
   * @throws IllegalArgumentException if there is no next character
   */
  private [this] def getCharOrDie(ci: Iterator[Char]): Char = {
    if (ci.hasNext) {
      ci.next()
    } else {
      throw new IllegalArgumentException("Expected escape sequence character, found nothing")
    }
  }

  override def encode(value: Any): String = {
    val array = value match {
      case byteArray: Array[Byte] => byteArray

      case byteBuffer: ByteBuffer if byteBuffer.hasArray => byteBuffer.array()

      case byteBuffer: ByteBuffer =>
        val arr = new Array[Byte](byteBuffer.remaining())
        byteBuffer.get(arr)
        arr

      case byteBuf: ByteBuf if byteBuf.hasArray => byteBuf.array()

      case byteBuf: ByteBuf =>
        val arr = new Array[Byte](byteBuf.readableBytes())
        byteBuf.getBytes(0, arr)
        arr
    }

    HexCodec.encode(array, HexStartChars)
  }

}
