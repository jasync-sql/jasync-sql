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

package com.github.mauricio.async.db.util

import org.jboss.netty.buffer.ChannelBuffer
import java.nio.charset.Charset

object ChannelWrapper {
  implicit def bufferToWrapper( buffer : ChannelBuffer ) = new ChannelWrapper(buffer)
}

class ChannelWrapper( val buffer : ChannelBuffer ) extends AnyVal {

  def readFixedString( length : Int, charset : Charset ) : String = {
    val result = buffer.toString(0, length, charset)
    buffer.readerIndex( buffer.readerIndex() + length )
    result
  }

  def readCString( charset : Charset ) = ChannelUtils.readCString(buffer, charset)

  def readUntilEOF( charset: Charset ) = ChannelUtils.readUntilEOF(buffer, charset)

}
