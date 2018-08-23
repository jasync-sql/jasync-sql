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

import collection.mutable.ArrayBuffer
import com.github.mauricio.async.db.{RowData, ResultSet}
import com.github.mauricio.async.db.util.Log

object MutableResultSet {
  val log = Log.get[MutableResultSet[Nothing]]
}

class MutableResultSet[T <: ColumnData](
                        val columnTypes: IndexedSeq[T]) extends ResultSet {

  private val rows = new ArrayBuffer[RowData]()
  private val columnMapping: Map[String, Int] = this.columnTypes.indices.map(
    index =>
      ( this.columnTypes(index).name, index ) ).toMap


  val columnNames : IndexedSeq[String] = this.columnTypes.map(c => c.name)

  val types : IndexedSeq[Int] = this.columnTypes.map(c => c.dataType)

  override def length: Int = this.rows.length

  override def apply(idx: Int): RowData = this.rows(idx)

  def addRow(row : Array[Any] ) {
    this.rows += new ArrayRowData(this.rows.size, this.columnMapping, row)
  }

}
