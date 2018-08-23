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

import com.github.mauricio.async.db.postgresql.column.{PostgreSQLColumnDecoderRegistry, ColumnTypes}
import com.github.mauricio.async.db.postgresql.messages.backend.PostgreSQLColumnData
import org.specs2.mutable.Specification
import io.netty.util.CharsetUtil
import io.netty.buffer.{Unpooled, ByteBuf}

class MutableResultSetSpec extends Specification {

  val charset = CharsetUtil.UTF_8
  val decoder = new PostgreSQLColumnDecoderRegistry

  def create(name: String, dataType: Int, columnNumber: Int = 0, dataTypeSize: Int = -1) = new PostgreSQLColumnData(
    name = name,
    tableObjectId = 0,
    columnNumber = columnNumber,
    dataType = dataType,
    dataTypeSize = dataTypeSize,
    dataTypeModifier = 0,
    fieldFormat = 0
  )

  "result set" should {

    "correctly map column data to fields" in {

      val columns = Array(
        create(
          name = "id",
          dataType = ColumnTypes.Integer,
          dataTypeSize = 4
        ),
        create(
          name = "name",
          columnNumber = 5,
          dataType = ColumnTypes.Varchar
        )
      )

      val text = "some data"
      val otherText = "some other data"

      val resultSet = new MutableResultSet(columns)

      resultSet.addRow(Array(1, text))
      resultSet.addRow(Array(2, otherText))

      resultSet(0)(0) === 1
      resultSet(0)("id") === 1

      resultSet(0)(1) === text
      resultSet(0)("name") === text

      resultSet(1)(0) === 2
      resultSet(1)("id") === 2

      resultSet(1)(1) === otherText
      resultSet(1)("name") === otherText

    }

    "should return the same order as the one given by columns" in {

      val columns = Array(
        create("id", ColumnTypes.Integer),
        create("name", ColumnTypes.Varchar),
        create("birthday", ColumnTypes.Date),
        create("created_at", ColumnTypes.Timestamp),
        create("updated_at", ColumnTypes.Timestamp)
      )
      val resultSet = new MutableResultSet(columns)

      resultSet.columnNames must contain(allOf("id", "name", "birthday", "created_at", "updated_at")).inOrder
    }

  }

}
