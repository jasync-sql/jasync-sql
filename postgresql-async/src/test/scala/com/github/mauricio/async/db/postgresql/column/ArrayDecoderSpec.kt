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

import com.github.mauricio.async.db.column.IntegerEncoderDecoder
import io.netty.buffer.Unpooled
import io.netty.util.CharsetUtil
import org.specs2.mutable.Specification

class ArrayDecoderSpec extends Specification {

  def execute( data : String ) : Any = {
    val numbers = data.getBytes( CharsetUtil.UTF_8 )
    val encoder = new ArrayDecoder(IntegerEncoderDecoder)
    encoder.decode(null, Unpooled.wrappedBuffer(numbers), CharsetUtil.UTF_8)
  }

  "encoder/decoder" should {

    "parse an array of numbers" in {
      execute("{1,2,3}") === List(1, 2, 3)
    }

    "parse an array of array of numbers" in {
      execute("{{1,2,3},{4,5,6}}") === List(List(1, 2, 3), List(4, 5, 6))
    }

  }

}
