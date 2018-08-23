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

import com.github.mauricio.async.db.RowData

class ArrayRowData(row : Int, val mapping : Map[String, Int], val columns : Array[Any]) extends RowData
{

  /**
   *
   * Returns a column value by it's position in the originating query.
   *
   * @param columnNumber
   * @return
   */
  def apply(columnNumber: Int): Any = columns(columnNumber)

  /**
   *
   * Returns a column value by it's name in the originating query.
   *
   * @param columnName
   * @return
   */
  def apply(columnName: String): Any = columns( mapping(columnName) )

  /**
   *
   * Number of this row in the query results. Counts start at 0.
   *
   * @return
   */
  def rowNumber: Int = row

  def length: Int = columns.length
}
