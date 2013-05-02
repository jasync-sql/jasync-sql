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

package com.github.mauricio.async.db.postgresql.util

import java.nio.charset.Charset
import org.jboss.netty.buffer.ChannelBuffer

object ChannelUtils {

  def writeLength(buffer: ChannelBuffer) {

    val length = buffer.writerIndex() - 1
    buffer.markWriterIndex()
    buffer.writerIndex(1)
    buffer.writeInt(length)

    buffer.resetWriterIndex()

  }

  /*
  def printBuffer(b: ChannelBuffer): Unit = {

    val bytes = new Array[Byte](b.readableBytes())
    b.markReaderIndex()
    b.readBytes(bytes)
    b.resetReaderIndex()

    println(bytes.mkString("-"))

  }*/

  def writeCString(content: String, b: ChannelBuffer, charset: Charset): Unit = {
    b.writeBytes(content.getBytes(charset))
    b.writeByte(0)
  }

  def readCString(b: ChannelBuffer, charset: Charset): String = {

    b.markReaderIndex()

    var byte: Byte = 0
    var count = 0

    do {
      byte = b.readByte()
      count += 1
    } while (byte != 0)

    b.resetReaderIndex()

    val result = b.toString(b.readerIndex(), count - 1, charset)

    b.readerIndex(b.readerIndex() + count)

    return result
  }

}
