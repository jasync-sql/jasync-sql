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
import com.github.mauricio.async.db.column.ColumnDecoderRegistry
import com.github.mauricio.async.db.util.Log
import com.github.mauricio.async.db.{RowData, ResultSet}
import java.nio.charset.Charset
import org.jboss.netty.buffer.ChannelBuffer

object MutableResultSet {
  val log = Log.get[MutableResultSet[ColumnData]]
}

class MutableResultSet[T <: ColumnData](
                        val columnTypes: IndexedSeq[T],
                        charset: Charset,
                        decoder : ColumnDecoderRegistry) extends ResultSet {

  private val rows = new ArrayBuffer[RowData]()
  private val columnMapping: List[(String, Int)] = this.columnTypes.indices.map(
    index =>
      ( this.columnTypes(index).name, index ) ).toList
    

  override def columnNames : IndexedSeq[String] = this.columnTypes.map( data => data.name )

  override def length: Int = this.rows.length

  override def apply(idx: Int): RowData = this.rows(idx)

  def addRawRow(row: Array[ChannelBuffer]) {
    val realRow = new ArrayRowData(columnMapping.size, this.rows.size, this.columnMapping)

    realRow.indices.foreach {
      index =>
        realRow(index) = if (row(index) == null) {
          null
        } else {
          this.decoder.decode( this.columnTypes(index).dataType, row(index).toString(charset) )
        }
    }

    this.rows += realRow
  }

  def addRawRow( row : Seq[String] ) {
    val realRow = new ArrayRowData(columnMapping.size, this.rows.size, this.columnMapping)

    realRow.indices.foreach {
      index =>
        realRow(index) = if (row(index) == null) {
          null
        } else {
          this.decoder.decode( this.columnTypes(index).dataType, row(index) )
        }
    }

    this.rows += realRow
  }

  def addRow( row : Seq[Any] ) {
    val realRow = new ArrayRowData( columnMapping.size, this.rows.size, this.columnMapping )
    var x = 0
    while ( x < row.size ) {
      realRow(x) = row(x)
      x += 1
    }
    this.rows += realRow
  }

}