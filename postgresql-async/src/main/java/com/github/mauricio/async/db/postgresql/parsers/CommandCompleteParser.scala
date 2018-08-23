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

import com.github.mauricio.async.db.postgresql.messages.backend.{CommandCompleteMessage, ServerMessage}
import com.github.mauricio.async.db.util.ByteBufferUtils
import java.nio.charset.Charset
import io.netty.buffer.ByteBuf

class CommandCompleteParser(charset: Charset) extends MessageParser {

  override def parseMessage(b: ByteBuf): ServerMessage = {

    val result = ByteBufferUtils.readCString(b, charset)

    val indexOfRowCount = result.lastIndexOf(" ")

    val rowCount = if (indexOfRowCount == -1) {
      0
    } else {
      try {
        result.substring(indexOfRowCount).trim.toInt
      } catch {
        case e: NumberFormatException => {
          0
        }
      }
    }

    new CommandCompleteMessage(rowCount, result)
  }

}
