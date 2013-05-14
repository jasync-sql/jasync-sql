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

import com.github.mauricio.async.db.mysql.message.server._
import com.github.mauricio.async.db.util.ChannelUtils
import com.github.mauricio.async.db.util.ChannelWrapper.bufferToWrapper
import org.jboss.netty.handler.codec.embedder.DecoderEmbedder
import org.jboss.netty.util.CharsetUtil
import org.specs2.mutable.Specification
import com.github.mauricio.async.db.mysql.message.server.OkMessage
import org.jboss.netty.buffer.ChannelBuffer

class MySQLFrameDecoderSpec extends Specification {

  final val charset = CharsetUtil.UTF_8

  "decoder" should {

    "decode an OK message correctly" in {

      val buffer = createOkPacket()

      val decoder = this.createPipeline()

      decoder.offer(buffer)

      val ok = decoder.peek().asInstanceOf[OkMessage]
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

      decoder.offer(buffer)

      val error = decoder.peek().asInstanceOf[ErrorMessage]

      error.errorCode === 27
      error.errorMessage === content
      error.sqlState === "HZAWAY"

    }

    "on a query process it should correctly send an OK" in {

      val decoder = new MySQLFrameDecoder(charset)
      val embedder = new DecoderEmbedder[ServerMessage](decoder)

      decoder.queryProcessStarted()

      decoder.isInQuery must beTrue
      decoder.processingColumns must beTrue

      val buffer = createOkPacket()

      embedder.offer(buffer)
      embedder.peek().asInstanceOf[OkMessage].message === "this is a test"

      decoder.isInQuery must beFalse
      decoder.processingColumns must beFalse
    }

    "on query process it should correctly send an error" in {

      val decoder = new MySQLFrameDecoder(charset)
      val embedder = new DecoderEmbedder[ServerMessage](decoder)

      decoder.queryProcessStarted()

      decoder.isInQuery must beTrue
      decoder.processingColumns must beTrue

      val content = "this is a crazy error"

      val buffer = createErrorPacket(content)

      embedder.offer(buffer)
      embedder.peek().asInstanceOf[ErrorMessage].errorMessage === content

      decoder.isInQuery must beFalse
      decoder.processingColumns must beFalse

    }

    "on query process it should correctly handle a result set" in {

      val decoder = new MySQLFrameDecoder(charset)
      val embedder = new DecoderEmbedder[ServerMessage](decoder)

      decoder.queryProcessStarted()

      decoder.totalColumns === 0

      val columnCountBuffer = ChannelUtils.packetBuffer()
      columnCountBuffer.writeLength(2)
      columnCountBuffer.writePacketLength()

      embedder.offer(columnCountBuffer)

      decoder.totalColumns === 2


    }

  }

  def createPipeline(): DecoderEmbedder[ServerMessage] = {
    new DecoderEmbedder[ServerMessage](new MySQLFrameDecoder(charset))
  }

  def createOkPacket() : ChannelBuffer = {
    val buffer = ChannelUtils.packetBuffer()
    buffer.writeByte(0)
    buffer.writeLength(10)
    buffer.writeLength(15)
    buffer.writeShort(5)
    buffer.writeShort(6)
    buffer.writeBytes("this is a test".getBytes(charset))
    buffer.writePacketLength()
    buffer
  }

  def createErrorPacket(content : String) : ChannelBuffer = {
    val buffer = ChannelUtils.packetBuffer()
    buffer.writeByte(0xff)
    buffer.writeShort(27)
    buffer.writeByte('H')
    buffer.writeBytes("ZAWAY".getBytes(charset))
    buffer.writeBytes(content.getBytes(charset))
    buffer.writePacketLength()
    buffer
  }


}
