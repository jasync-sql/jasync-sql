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

package com.github.mauricio.async.db.postgresql.parsers

import com.github.jasync.sql.db.util.ByteBufferUtils
import com.github.mauricio.async.db.postgresql.messages.backend.ServerMessage
import com.github.mauricio.async.db.util.ByteBufferUtils
import java.nio.charset.Charset
import io.netty.buffer.ByteBuf

abstract class InformationParser(val charset: Charset) : MessageParser {

  override fun parseMessage(b: ByteBuf): ServerMessage {

    val fields = mutableMapOf<Char, String>()
    while (b.isReadable) {
      val kind = b.readByte()
      if (kind.toInt() != 0) {
        fields[kind.toChar()] = ByteBufferUtils.readCString(b, charset)
      }
    }

    return createMessage(fields.toMap())
  }

  private fun createMessage(fields: Map<Char, String>) = ServerMessage()

}
