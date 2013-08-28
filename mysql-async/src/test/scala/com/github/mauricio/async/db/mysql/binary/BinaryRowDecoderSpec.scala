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

import io.netty.buffer.Unpooled
import io.netty.util.CharsetUtil
import com.github.mauricio.async.db.mysql.column.ColumnTypes
import com.github.mauricio.async.db.mysql.message.server.ColumnDefinitionMessage
import org.specs2.mutable.Specification
import java.nio.ByteOrder
import com.github.mauricio.async.db.mysql.codec.DecoderRegistry

class BinaryRowDecoderSpec extends Specification {

  val registry = new DecoderRegistry(CharsetUtil.UTF_8)
  val decoder = new BinaryRowDecoder()

  val idAndName = Array[Byte]( 0, 1, 0, 0, 0, 0, 0, 0, 0, 3, 106, 111, 101)
  val idAndNameColumns = Array(
    createColumn("id", ColumnTypes.FIELD_TYPE_LONGLONG),
    createColumn("name", ColumnTypes.FIELD_TYPE_VAR_STRING) )

  val idNameAndNull = Array[Byte]( 16, 1, 0, 0, 0, 0, 0, 0, 0, 3, 106, 111, 101)
  val idNameAndNullColumns = idAndNameColumns ++ List( createColumn("null_value", ColumnTypes.FIELD_TYPE_NULL) )

  "binary row decoder" should {

    "decoder a long and a string from the byte array" in {

      val buffer = Unpooled.wrappedBuffer(idAndName).order(ByteOrder.LITTLE_ENDIAN)
      val result = decoder.decode(buffer, idAndNameColumns)
      buffer.release()
      result(0) === 1L
      result(1) === "joe"

    }

    "decode a row with an long, a string and a null" in {
      val buffer = Unpooled.wrappedBuffer(idNameAndNull).order(ByteOrder.LITTLE_ENDIAN)
      val result = decoder.decode(buffer, idNameAndNullColumns)
      buffer.release()
      result(0) === 1L
      result(1) === "joe"
      result(2) must beNull
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
      0,
      registry.binaryDecoderFor(columnType, 3),
      registry.textDecoderFor(columnType, 3)
    )

  }

}
