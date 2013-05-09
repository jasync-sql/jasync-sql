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

package com.github.mauricio.async.db.mysql.codec

import com.github.mauricio.async.db.exceptions.{BufferNotFullyConsumedException, ParserNotAvailableException}
import com.github.mauricio.async.db.mysql.MySQLHelper
import com.github.mauricio.async.db.mysql.decoder._
import com.github.mauricio.async.db.mysql.message.server.ServerMessage
import com.github.mauricio.async.db.util.ChannelUtils.read3BytesInt
import com.github.mauricio.async.db.util.Log
import java.nio.charset.Charset
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.{Channel, ChannelHandlerContext}
import org.jboss.netty.handler.codec.frame.FrameDecoder
import com.github.mauricio.async.db.util.ChannelWrapper.bufferToWrapper

object MySQLFrameDecoder {
  val log = Log.get[MySQLFrameDecoder]
}

class MySQLFrameDecoder(charset: Charset) extends FrameDecoder {

  import MySQLFrameDecoder.log

  private final val handshakeDecoder = new HandshakeV10Decoder(charset)
  private final val errorDecoder = new ErrorDecoder(charset)
  private final val okDecoder = new OkDecoder(charset)
  private final val columnDecoder = new ColumnDefinitionDecoder(charset)
  private final val rowDecoder = new ResultSetRowDecoder(charset)

  private var processingColumns = false
  private var isInQuery = false
  private var totalColumns: Long = 0
  private var processedColumns: Long = 0

  def decode(ctx: ChannelHandlerContext, channel: Channel, buffer: ChannelBuffer): AnyRef = {
    if (buffer.readableBytes() > 4) {

      buffer.markReaderIndex()

      val size = read3BytesInt(buffer)
      val sequence = buffer.readByte()

      if (buffer.readableBytes() >= size) {

        val messageType = buffer.getByte(buffer.readerIndex())

        val slice = buffer.readSlice(size)

        //val requestDump = MySQLHelper.dumpAsHex(slice, slice.readableBytes())
        //log.debug(s"Server message is type ${"%02x".format(messageType)} ( $messageType - $size bytes)\n${requestDump}")

        // removing initial kind byte so that we can switch
        // on known messages but add it back if this is a query process
        slice.readByte()

        val decoder = messageType match {
          case ServerMessage.ServerProtocolVersion => this.handshakeDecoder
          case ServerMessage.Error => {
            this.clear
            this.errorDecoder
          }
          case ServerMessage.EOF => {
            if ( this.processingColumns ) {
              this.processingColumns = false
              ColumnProcessingFinishedDecoder
            } else {
              this.clear
              EOFMessageDecoder
            }
          }
          case ServerMessage.Ok => {
            this.clear
            this.okDecoder
          }
          case _ => {

            if ( this.isInQuery ) {
              null
            } else {
              throw new ParserNotAvailableException(messageType)
            }

          }
        }

        if ( decoder == null ) {
          slice.readerIndex( slice.readerIndex() - 1 )
          return decodeQueryResult(slice, buffer)
        } else {
          val result = decoder.decode(slice)

          if (slice.readableBytes() != 0) {
            throw new BufferNotFullyConsumedException(slice)
          }

          return result
        }

      } else {
        buffer.resetReaderIndex()
      }

    }

    return null
  }

  def queryProcessStarted() {
    this.isInQuery = true
    this.processingColumns = true
  }

  private def decodeQueryResult( slice : ChannelBuffer, buffer : ChannelBuffer ) : AnyRef = {
    if ( this.totalColumns == 0 ) {
      this.totalColumns = slice.readBinaryLength
      return null
    } else {
      if ( this.totalColumns == this.processedColumns ) {
        this.rowDecoder.decode(slice)
      } else {
        this.processedColumns += 1
        this.columnDecoder.decode(slice)
      }
    }
  }

  private def clear {
    this.isInQuery = false
    this.processingColumns = false
    this.totalColumns = 0
    this.processedColumns = 0
  }

}
