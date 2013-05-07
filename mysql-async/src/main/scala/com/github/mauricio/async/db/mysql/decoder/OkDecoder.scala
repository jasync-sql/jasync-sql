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

package com.github.mauricio.async.db.mysql.decoder

import org.jboss.netty.buffer.ChannelBuffer
import com.github.mauricio.async.db.mysql.message.server.{OkMessage, ServerMessage}
import java.nio.charset.Charset
import com.github.mauricio.async.db.util.ChannelWrapper.bufferToWrapper

class OkDecoder( charset : Charset ) extends MessageDecoder {

  /*
  1              [00] the OK header
  lenenc-int     affected rows
  lenenc-int     last-insert-id
    if capabilities & CLIENT_PROTOCOL_41 {
  2              status_flags
  2              warnings
    } elseif capabilities & CLIENT_TRANSACTIONS {
  2              status_flags
    }
  string[EOF]    info
   */

  def decode(buffer: ChannelBuffer): ServerMessage = {

    new OkMessage(
      buffer.readBinaryLength,
      buffer.readBinaryLength,
      buffer.readShort(),
      buffer.readShort(),
      buffer.readUntilEOF(charset)
    )

  }

}
