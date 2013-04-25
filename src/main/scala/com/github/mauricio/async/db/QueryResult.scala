/*
 * Copyright 2013 Maurício Linhares
 *
 * Maurício Linhares licenses this file to you under the Apache License,
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

package com.github.mauricio.async.db

/**
 *
 * This is the result of the execution of a statement, contains basic information as the number or rows
 * affected by the statement and the rows returned if there were any.
 *
 * @param rowsAffected
 * @param statusMessage
 * @param rows
 */

class QueryResult(val rowsAffected: Int, val statusMessage: String, val rows: Option[ResultSet]) {

  override def toString: String = {
    "QueryResult{rows -> %s,status -> %s}".format(this.rowsAffected, this.statusMessage)
  }

}
