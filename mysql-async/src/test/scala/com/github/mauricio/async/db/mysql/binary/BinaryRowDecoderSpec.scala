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

package com.github.mauricio.async.db.mysql.binary

import com.github.mauricio.async.db.mysql.column.ColumnTypes
import com.github.mauricio.async.db.mysql.message.server.ColumnDefinitionMessage
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil
import org.specs2.mutable.Specification
import java.nio.ByteOrder

class BinaryRowDecoderSpec extends Specification {

  val idAndName = Array[Byte](0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 3, 106, 111, 101)
  val idAndNameColumns = Array(
    createColumn("id", ColumnTypes.FIELD_TYPE_LONGLONG),
    createColumn("name", ColumnTypes.FIELD_TYPE_VAR_STRING) )
  val decoder = new BinaryRowDecoder(CharsetUtil.UTF_8)

  "binary row decoder" should {

    "decoder a long and a string from the byte array" in {

      val buffer = ChannelBuffers.wrappedBuffer(ByteOrder.LITTLE_ENDIAN, idAndName)
      val result = decoder.decode(buffer, idAndNameColumns)

      result(0) === 1L
      result(1) === "joe"

    }

  }

  def createColumn( name : String, columnType : Int ) : ColumnDefinitionMessage = {

    new ColumnDefinitionMessage(
      "root",
      "root",
      "users",
      "users",
      name,
      name,
      -1,
      0,
      columnType,
      0,
      0
    )

  }

}
