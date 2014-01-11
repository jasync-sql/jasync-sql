/*
 * Copyright 2013-2014 db-async-common
 *
 * The db-async-common project licenses this file to you under the Apache License,
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

import language.implicitConversions
import java.nio.charset.Charset
import io.netty.buffer.ByteBuf
import com.github.mauricio.async.db.postgresql.messages.backend.{NotificationResponse, ServerMessage}
import com.github.mauricio.async.db.util.ChannelWrapper.bufferToWrapper

class NotificationResponseParser( charset : Charset ) extends MessageParser {

  def parseMessage(buffer: ByteBuf): ServerMessage = {
    new NotificationResponse( buffer.readInt(), buffer.readCString(charset), buffer.readCString(charset) )
  }

}