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

package com.github.mauricio.async.db.mysql.message.server

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import org.jboss.netty.buffer.ChannelBuffer

class ResultSetRowMessage
  extends ServerMessage( ServerMessage.Row )
  with mutable.Buffer[ChannelBuffer]
{

  private val buffer = new ArrayBuffer[ChannelBuffer]()

  def length: Int = buffer.length

  def apply(idx: Int): ChannelBuffer = buffer(idx)

  def update(n: Int, newelem: ChannelBuffer) {
    buffer.update(n, newelem)
  }

  def +=(elem: ChannelBuffer): this.type = {
    this.buffer += elem
    this
  }

  def clear() {
    this.buffer.clear()
  }

  def +=:(elem: ChannelBuffer): this.type = {
    this.buffer.+=:(elem)
    this
  }

  def insertAll(n: Int, elems: Traversable[ChannelBuffer]) {
    this.buffer.insertAll(n, elems)
  }

  def remove(n: Int): ChannelBuffer = {
    this.buffer.remove(n)
  }

  override def iterator: Iterator[ChannelBuffer] = this.buffer.iterator

}