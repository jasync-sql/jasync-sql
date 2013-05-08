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

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable

class ResultSetRowMessage
  extends ServerMessage( ServerMessage.Row )
  with mutable.Buffer[Any]
{

  private val buffer = new ArrayBuffer[Any]()

  def length: Int = buffer.length

  def apply(idx: Int): Any = buffer(idx)

  def update(n: Int, newelem: Any) {
    buffer.update(n, newelem)
  }

  def +=(elem: Any): this.type = {
    this.buffer += elem
    this
  }

  def clear() {
    this.buffer.clear()
  }

  def +=:(elem: Any): this.type = {
    this.buffer.+=:(elem)
    this
  }

  def insertAll(n: Int, elems: Traversable[Any]) {
    this.buffer.insertAll(n, elems)
  }

  def remove(n: Int): Any = {
    this.buffer.remove(n)
  }

  def iterator: Iterator[Any] = this.buffer.iterator

}