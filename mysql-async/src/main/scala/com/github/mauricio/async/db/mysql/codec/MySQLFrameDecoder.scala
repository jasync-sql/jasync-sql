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
import com.github.mauricio.async.db.mysql.decoder._
import com.github.mauricio.async.db.mysql.message.server.{BinaryRowMessage, ColumnProcessingFinishedMessage, PreparedStatementPrepareResponse, ServerMessage}
import com.github.mauricio.async.db.util.ChannelUtils.read3BytesInt
import com.github.mauricio.async.db.util.ChannelWrapper.bufferToWrapper
import com.github.mauricio.async.db.util.{PrintUtils, Log}
import java.nio.charset.Charset
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.{Channel, ChannelHandlerContext}
import org.jboss.netty.handler.codec.frame.FrameDecoder
import com.github.mauricio.async.db.mysql.message.client.PreparedStatementPrepareMessage
import com.github.mauricio.async.db.mysql.MySQLHelper

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
  private final val preparedStatementPrepareDecoder = new PreparedStatementPrepareResponseDecoder()

  private var processingColumns = false
  private var processingParams = false
  private var isInQuery = false
  private var isPreparedStatementPrepare = false
  private var isPreparedStatementExecute = false
  private var isPreparedStatementExecuteRows = false

  private var totalParams = 0L
  private var processedParams = 0L
  private var totalColumns = 0L
  private var processedColumns = 0L

  def decode(ctx: ChannelHandlerContext, channel: Channel, buffer: ChannelBuffer): AnyRef = {
    if (buffer.readableBytes() > 4) {

      //val requestDump = MySQLHelper.dumpAsHex(buffer, buffer.readableBytes())
      //log.debug(s"Server message\n${requestDump}")
      //PrintUtils.printArray( "any message", buffer)

      buffer.markReaderIndex()

      val size = read3BytesInt(buffer)
      val sequence = buffer.readUnsignedByte()

      if (buffer.readableBytes() >= size) {

        val messageType = buffer.getByte(buffer.readerIndex())

        val slice = buffer.readSlice(size)

        //val dump = MySQLHelper.dumpAsHex(slice, slice.readableBytes())
        //log.debug(s"Message type $messageType - message size - $size - sequence - $sequence\n$dump")

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

            if (this.processingParams && this.totalParams > 0) {
              this.processingParams = false
              ParamProcessingFinishedDecoder
            } else {
              if (this.processingColumns) {
                this.processingColumns = false
                ColumnProcessingFinishedDecoder
              } else {
                this.clear
                EOFMessageDecoder
              }
            }

          }
          case ServerMessage.Ok => {
            if (this.isPreparedStatementPrepare) {
              this.preparedStatementPrepareDecoder
            } else {
              if ( this.isPreparedStatementExecuteRows ) {
                null
              } else {
                this.clear
                this.okDecoder
              }
            }
          }
          case _ => {

            if (this.isInQuery) {
              null
            } else {
              throw new ParserNotAvailableException(messageType)
            }

          }
        }

        if (decoder == null) {
          slice.readerIndex(slice.readerIndex() - 1)
          val result = decodeQueryResult(slice)

          if (slice.readableBytes() != 0) {
            throw new BufferNotFullyConsumedException(slice)
          }

          return result
        } else {
          val result = decoder.decode(slice)

          result match {
            case m : PreparedStatementPrepareResponse => {
              this.totalColumns = m.columnsCount
              this.totalParams = m.paramsCount
            }
            case m : ColumnProcessingFinishedMessage if this.isPreparedStatementPrepare => {
              this.clear
            }
            case m : ColumnProcessingFinishedMessage if this.isPreparedStatementExecute => {
              this.isPreparedStatementExecuteRows = true
            }
            case _ =>
          }

          if (result.isInstanceOf[PreparedStatementPrepareResponse]) {
            val message = result.asInstanceOf[PreparedStatementPrepareResponse]
            this.totalColumns = message.columnsCount
            this.totalParams = message.paramsCount
          }

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

  def preparedStatementPrepareStarted() {
    this.processingParams = true
    this.processingColumns = true
    this.isPreparedStatementPrepare = true
    this.queryProcessStarted()
  }

  def preparedStatementExecuteStarted() {
    this.queryProcessStarted()
    this.isPreparedStatementExecute = true
    this.processingParams = false
  }

  def queryProcessStarted() {
    this.isInQuery = true
    this.processingColumns = true
  }

  private def decodeQueryResult(slice: ChannelBuffer): AnyRef = {
    if (this.totalColumns == 0) {
      this.totalColumns = slice.readBinaryLength
      return null
    } else {

      if (this.totalParams != this.processedParams) {
        this.processedParams += 1
        this.columnDecoder.decode(slice)
      } else {
        if (this.totalColumns == this.processedColumns) {
          if ( this.isPreparedStatementExecute ) {
            new BinaryRowMessage(slice.readSlice(slice.readableBytes()))
          } else {
            this.rowDecoder.decode(slice)
          }
        } else {
          this.processedColumns += 1
          this.columnDecoder.decode(slice)
        }
      }

    }
  }

  private def clear {
    this.isPreparedStatementPrepare = false
    this.isPreparedStatementExecute = false
    this.isPreparedStatementExecuteRows = false
    this.isInQuery = false
    this.processingColumns = false
    this.totalColumns = 0
    this.processedColumns = 0
    this.totalParams = 0
    this.processedParams = 0
  }

}
