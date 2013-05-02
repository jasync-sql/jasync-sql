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

package com.github.mauricio.async.db.general

import com.github.mauricio.async.db.postgresql.column.{DefaultColumnDecoderRegistry, ColumnTypes}
import com.github.mauricio.async.db.postgresql.messages.backend.ColumnData
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}
import org.jboss.netty.util.CharsetUtil
import org.specs2.mutable.Specification
import com.github.mauricio.async.db.postgresql.general.MutableResultSet

class MutableResultSetSpec extends Specification {

  val charset = CharsetUtil.UTF_8
  val decoder = new DefaultColumnDecoderRegistry

  "result set" should {

    "correctly map column data to fields" in {

      val columns = Array(
        new ColumnData(
          name = "id",
          tableObjectId = 0,
          columnNumber = 0,
          dataType = ColumnTypes.Integer,
          dataTypeSize = 4,
          dataTypeModifier = 0,
          fieldFormat = 0
        ),
        new ColumnData(
          name = "name",
          tableObjectId = 0,
          columnNumber = 5,
          dataType = ColumnTypes.Varchar,
          dataTypeSize = -1,
          dataTypeModifier = 0,
          fieldFormat = 0
        )
      )

      val text = "some data"
      val otherText = "some other data"

      val resultSet = new MutableResultSet(columns, charset, decoder)

      resultSet.addRawRow( Array( toBuffer(1), toBuffer( text ) ) )
      resultSet.addRawRow( Array( toBuffer(2), toBuffer( otherText ) ) )

      resultSet(0)(0) === 1
      resultSet(0)("id") === 1

      resultSet(0)(1) === text
      resultSet(0)("name") === text

      resultSet(1)(0) === 2
      resultSet(1)("id") === 2

      resultSet(1)(1) === otherText
      resultSet(1)("name") === otherText

    }

  }

  def toBuffer( content : String ) : ChannelBuffer = {
    val buffer = ChannelBuffers.dynamicBuffer()
    buffer.writeBytes( content.getBytes(charset) )
    buffer
  }

  def toBuffer( value : Int ) : ChannelBuffer = {
    val buffer = ChannelBuffers.dynamicBuffer()
    buffer.writeBytes(value.toString.getBytes(charset))
    buffer
  }

}
