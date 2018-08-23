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

import io.netty.buffer.ByteBuf
import io.netty.util.CharsetUtil
import com.github.mauricio.async.db.mysql.message.server._
import com.github.mauricio.async.db.util.ByteBufferUtils
import com.github.mauricio.async.db.util.ChannelWrapper.bufferToWrapper
import org.specs2.mutable.Specification
import com.github.mauricio.async.db.mysql.message.server.OkMessage
import com.github.mauricio.async.db.mysql.column.ColumnTypes
import io.netty.channel.embedded.EmbeddedChannel

class MySQLFrameDecoderSpec extends Specification {

  final val charset = CharsetUtil.UTF_8

  "decoder" should {

    "decode an OK message correctly" in {

      val buffer = createOkPacket()

      val decoder = this.createPipeline()

      decoder.writeInbound(buffer)

      val ok = decoder.readInbound().asInstanceOf[OkMessage]
      ok.affectedRows === 10
      ok.lastInsertId === 15
      ok.message === "this is a test"
      ok.statusFlags === 5
      ok.warnings === 6
    }

    "decode an error message" in {

      val content = "this is the error message"

      val buffer = createErrorPacket(content)

      val decoder = createPipeline()

      decoder.writeInbound(buffer)

      val error = decoder.readInbound().asInstanceOf[ErrorMessage]

      error.errorCode === 27
      error.errorMessage === content
      error.sqlState === "HZAWAY"

    }

    "on a query process it should correctly send an OK" in {

      val decoder = new MySQLFrameDecoder(charset, "[mysql-connection]")
      decoder.hasDoneHandshake = true
      val embedder = new EmbeddedChannel(decoder)
      embedder.config.setAllocator(LittleEndianByteBufAllocator.INSTANCE)

      decoder.queryProcessStarted()

      decoder.isInQuery must beTrue
      decoder.processingColumns must beTrue

      val buffer = createOkPacket()

      embedder.writeInbound(buffer) must beTrue
      embedder.readInbound().asInstanceOf[OkMessage].message === "this is a test"

      decoder.isInQuery must beFalse
      decoder.processingColumns must beFalse
    }

    "on query process it should correctly send an error" in {

      val decoder = new MySQLFrameDecoder(charset, "[mysql-connection]")
      decoder.hasDoneHandshake = true
      val embedder = new EmbeddedChannel(decoder)
      embedder.config.setAllocator(LittleEndianByteBufAllocator.INSTANCE)

      decoder.queryProcessStarted()

      decoder.isInQuery must beTrue
      decoder.processingColumns must beTrue

      val content = "this is a crazy error"

      val buffer = createErrorPacket(content)

      embedder.writeInbound(buffer) must beTrue
      embedder.readInbound().asInstanceOf[ErrorMessage].errorMessage === content

      decoder.isInQuery must beFalse
      decoder.processingColumns must beFalse

    }

    "on query process it should correctly handle a result set" in {

      val decoder = new MySQLFrameDecoder(charset, "[mysql-connection]")
      decoder.hasDoneHandshake = true
      val embedder = new EmbeddedChannel(decoder)
      embedder.config.setAllocator(LittleEndianByteBufAllocator.INSTANCE)

      decoder.queryProcessStarted()

      decoder.totalColumns === 0

      val columnCountBuffer = ByteBufferUtils.packetBuffer()
      columnCountBuffer.writeLength(2)
      columnCountBuffer.writePacketLength()

      embedder.writeInbound(columnCountBuffer)

      decoder.totalColumns === 2

      val columnId = createColumnPacket("id", ColumnTypes.FIELD_TYPE_LONG)
      val columnName = createColumnPacket("name", ColumnTypes.FIELD_TYPE_VARCHAR)

      embedder.writeInbound(columnId)

      embedder.readInbound().asInstanceOf[ColumnDefinitionMessage].name === "id"

      decoder.processedColumns === 1

      embedder.writeInbound(columnName)

      embedder.readInbound().asInstanceOf[ColumnDefinitionMessage].name === "name"

      decoder.processedColumns === 2

      embedder.writeInbound(this.createEOFPacket())

      embedder.readInbound().asInstanceOf[ColumnProcessingFinishedMessage].eofMessage.flags === 8765

      decoder.processingColumns must beFalse

      val row = ByteBufferUtils.packetBuffer()
      row.writeLenghtEncodedString("1", charset)
      row.writeLenghtEncodedString("some name", charset)
      row.writePacketLength()

      embedder.writeInbound(row)

      embedder.readInbound().isInstanceOf[ResultSetRowMessage] must beTrue

      embedder.writeInbound(this.createEOFPacket())

      decoder.isInQuery must beFalse
    }

  }

  def createPipeline(): EmbeddedChannel = {
    val decoder = new MySQLFrameDecoder(charset, "[mysql-connection]")
    decoder.hasDoneHandshake = true
    val channel = new EmbeddedChannel(decoder)
    channel.config.setAllocator(LittleEndianByteBufAllocator.INSTANCE)
    channel
  }

  def createOkPacket() : ByteBuf = {
    val buffer = ByteBufferUtils.packetBuffer()
    buffer.writeByte(0)
    buffer.writeLength(10)
    buffer.writeLength(15)
    buffer.writeShort(5)
    buffer.writeShort(6)
    buffer.writeBytes("this is a test".getBytes(charset))
    buffer.writePacketLength()
    buffer
  }

  def createErrorPacket(content : String) : ByteBuf = {
    val buffer = ByteBufferUtils.packetBuffer()
    buffer.writeByte(0xff)
    buffer.writeShort(27)
    buffer.writeByte('H')
    buffer.writeBytes("ZAWAY".getBytes(charset))
    buffer.writeBytes(content.getBytes(charset))
    buffer.writePacketLength()
    buffer
  }

  def createColumnPacket( name : String, columnType : Int ) : ByteBuf = {
    val buffer = ByteBufferUtils.packetBuffer()
    buffer.writeLenghtEncodedString("def", charset)
    buffer.writeLenghtEncodedString("some_schema", charset)
    buffer.writeLenghtEncodedString("some_table", charset)
    buffer.writeLenghtEncodedString("some_table", charset)
    buffer.writeLenghtEncodedString(name, charset)
    buffer.writeLenghtEncodedString(name, charset)
    buffer.writeLength(12)
    buffer.writeShort(0x03)
    buffer.writeInt(10)
    buffer.writeByte(columnType)
    buffer.writeShort(76)
    buffer.writeByte(0)
    buffer.writeShort(56)
    buffer.writePacketLength()
    buffer
  }

  def createEOFPacket() : ByteBuf = {
    val buffer = ByteBufferUtils.packetBuffer()
    buffer.writeByte(0xfe)
    buffer.writeShort(879)
    buffer.writeShort(8765)

    buffer.writePacketLength()

    buffer
  }



}
