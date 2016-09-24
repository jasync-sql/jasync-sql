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

import com.github.mauricio.async.db.column.ColumnDecoder
import com.github.mauricio.async.db.postgresql.util.{ArrayStreamingParserDelegate, ArrayStreamingParser}
import scala.collection.IndexedSeq
import scala.collection.mutable.ArrayBuffer
import com.github.mauricio.async.db.general.ColumnData
import io.netty.buffer.{Unpooled, ByteBuf}
import java.nio.charset.Charset

class ArrayDecoder(private val decoder: ColumnDecoder) extends ColumnDecoder {

  override def decode( kind : ColumnData, buffer : ByteBuf, charset : Charset ): IndexedSeq[Any] = {

    val bytes = new Array[Byte](buffer.readableBytes())
    buffer.readBytes(bytes)
    val value = new String(bytes, charset)

    var stack = List.empty[ArrayBuffer[Any]]
    var current: ArrayBuffer[Any] = null
    var result: IndexedSeq[Any] = null
    val delegate = new ArrayStreamingParserDelegate {
      override def arrayEnded {
        result = stack.head
        stack = stack.tail
      }

      override def elementFound(element: String) {
        val result = if ( decoder.supportsStringDecoding ) {
          decoder.decode(element)
        } else {
          decoder.decode(kind, Unpooled.wrappedBuffer( element.getBytes(charset) ), charset)
        }
        current += result
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

        stack ::= current
      }
    }

    ArrayStreamingParser.parse(value, delegate)

    result
  }

  def decode( value : String ) : Any = throw new UnsupportedOperationException("Should not be called")

}
