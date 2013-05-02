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

package com.github.mauricio.async.db.postgresql.column

import scala.collection.IndexedSeq
import scala.collection.mutable.{ArrayBuffer, Stack}
import com.github.mauricio.async.db.postgresql.util.{ArrayStreamingParserDelegate, ArrayStreamingParser}

class ArrayDecoder(private val encoder: ColumnDecoder) extends ColumnDecoder {

  override def decode(value: String): IndexedSeq[Any] = {

    val stack = new Stack[ArrayBuffer[Any]]()
    var current: ArrayBuffer[Any] = null
    var result: IndexedSeq[Any] = null
    val delegate = new ArrayStreamingParserDelegate {
      override def arrayEnded {
        result = stack.pop()
      }

      override def elementFound(element: String) {
        current += encoder.decode(element)
      }

      override def nullElementFound {
        current += null
      }

      override def arrayStarted {
        current = new ArrayBuffer[Any]()

        stack.headOption match {
          case Some(item) => {
            item += current
          }
          case None => {}
        }

        stack.push(current)
      }
    }

    ArrayStreamingParser.parse(value, delegate)

    result
  }

}
