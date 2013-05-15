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

package com.github.mauricio.async.db.mysql.column

import com.github.mauricio.async.db.column.ColumnDecoder
import org.jboss.netty.buffer.ChannelBuffer
import java.nio.charset.Charset

object ByteArrayColumnDecoder extends ColumnDecoder {

  override def decode(value: ChannelBuffer, charset: Charset): Any = {
    val bytes = new Array[Byte](value.readableBytes())
    value.readBytes(bytes)
    value
  }

  def decode(value: String): Any = {
    throw new UnsupportedOperationException("This method should never be called for byte arrays")
  }
}
