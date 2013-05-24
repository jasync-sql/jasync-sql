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

import com.github.mauricio.async.db.exceptions._
import com.github.mauricio.async.db.mysql.decoder._
import com.github.mauricio.async.db.mysql.message.server._
import com.github.mauricio.async.db.util.ChannelUtils.read3BytesInt
import com.github.mauricio.async.db.util.ChannelWrapper.bufferToWrapper
import com.github.mauricio.async.db.util.Log
import java.nio.charset.Charset
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.{Channel, ChannelHandlerContext}
import org.jboss.netty.handler.codec.frame.FrameDecoder
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

  private[codec] var processingColumns = false
  private[codec] var processingParams = false
  private[codec] var isInQuery = false
  private[codec] var isPreparedStatementPrepare = false
  private[codec] var isPreparedStatementExecute = false
  private[codec] var isPreparedStatementExecuteRows = false

  private[codec] var totalParams = 0L
  private[codec] var processedParams = 0L
  private[codec] var totalColumns = 0L
  private[codec] var processedColumns = 0L

  private var hasReadColumnsCount = false

  def decode(ctx: ChannelHandlerContext, channel: Channel, buffer: ChannelBuffer): AnyRef = {
    if (buffer.readableBytes() > 4) {

      buffer.markReaderIndex()

      val size = read3BytesInt(buffer)

      val sequence = buffer.readUnsignedByte() // we have to read this

      if (buffer.readableBytes() >= size) {

        val messageType = buffer.getByte(buffer.readerIndex())

        if (size < 0) {
          throw new NegativeMessageSizeException(messageType, size)
        }

        val slice = buffer.readSlice(size)

        //val dump = MySQLHelper.dumpAsHex(slice)

        //log.debug(s"Dump of message is - $messageType - $size isInQuery $isInQuery processingColumns $processingColumns processedColumns $processedColumns processingParams $processingParams processedParams $processedParams \n{}", dump)

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
              if (this.totalColumns == 0) {
                ParamAndColumnProcessingFinishedDecoder
              } else {
                ParamProcessingFinishedDecoder
              }
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
              if (this.isPreparedStatementExecuteRows) {
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
            case m: PreparedStatementPrepareResponse => {
              this.hasReadColumnsCount = true
              this.totalColumns = m.columnsCount
              this.totalParams = m.paramsCount
            }
            case m: ParamAndColumnProcessingFinishedMessage => {
              this.clear
            }
            case m: ColumnProcessingFinishedMessage if this.isPreparedStatementPrepare => {
              this.clear
            }
            case m: ColumnProcessingFinishedMessage if this.isPreparedStatementExecute => {
              this.isPreparedStatementExecuteRows = true
            }
            case _ =>
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

  private def decodeQueryResult(slice: ChannelBuffer): AnyRef = {
    if (!hasReadColumnsCount) {
      this.hasReadColumnsCount = true
      this.totalColumns = slice.readBinaryLength
      return null
    }

    if (this.processingParams && this.totalParams != this.processedParams) {
      this.processedParams += 1
      return this.columnDecoder.decode(slice)
    }


    if (this.totalColumns == this.processedColumns) {
      if (this.isPreparedStatementExecute) {
        val row = slice.readSlice(slice.readableBytes())
        row.readByte() // reads initial 00 at message
        new BinaryRowMessage(row)
      } else {
        this.rowDecoder.decode(slice)
      }
    } else {
      this.processedColumns += 1
      this.columnDecoder.decode(slice)
    }

  }

  def preparedStatementPrepareStarted() {
    this.queryProcessStarted()
    this.hasReadColumnsCount = true
    this.processingParams = true
    this.processingColumns = true
    this.isPreparedStatementPrepare = true
  }

  def preparedStatementExecuteStarted(columnsCount: Int, paramsCount: Int) {
    this.queryProcessStarted()
    this.hasReadColumnsCount = false
    this.totalColumns = columnsCount
    this.totalParams = paramsCount
    this.isPreparedStatementExecute = true
    this.processingParams = false
  }

  def queryProcessStarted() {
    this.isInQuery = true
    this.processingColumns = true
    this.hasReadColumnsCount = false
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
    this.hasReadColumnsCount = false
  }

}
