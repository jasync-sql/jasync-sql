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

package com.github.mauricio.async.db.postgresql

import collection.mutable.ArrayBuffer
import com.github.mauricio.async.db.ResultSet
import com.github.mauricio.async.db.postgresql.messages.backend.ColumnData
import com.github.mauricio.async.db.util.Log
import java.nio.charset.Charset
import org.jboss.netty.buffer.ChannelBuffer
import com.github.mauricio.async.db.postgresql.column.ColumnDecoderRegistry

object MutableQuery {
  val log = Log.get[MutableQuery]
}

class MutableQuery(val columnTypes: Array[ColumnData], charset: Charset, decoder : ColumnDecoderRegistry) extends ResultSet {

  private val rows = new ArrayBuffer[Array[Any]]()
  private val columnMapping: Map[String, Int] = this.columnTypes.map {
    columnData =>
      (columnData.name, columnData.columnNumber - 1)
  }.toMap

  override def length: Int = this.rows.length

  override def apply(idx: Int): Array[Any] = this.rows(idx)

  def update(idx: Int, elem: Array[Any]) = this.rows(idx) = elem

  def addRawRow(row: Array[ChannelBuffer]) {

    val realRow = new Array[Any](row.length)

    0.until(row.length).foreach {
      index =>

        realRow(index) = if (row(index) == null) {
          null
        } else {
          this.decoder.decode( this.columnTypes(index).dataType, row(index).toString(charset) )
        }

    }

    this.rows += realRow
  }

  def getValue(columnNumber: Int, rowNumber: Int): Any = {
    this.rows(rowNumber)(columnNumber)
  }

  def getValue(columnName: String, rowNumber: Int): Any = {
    this.rows(rowNumber)(this.columnMapping(columnName))
  }

  def apply(name: String, row: Int): Any = this.getValue(name, row)

  def apply(column: Int, row: Int): Any = this.getValue(column, row)

}
