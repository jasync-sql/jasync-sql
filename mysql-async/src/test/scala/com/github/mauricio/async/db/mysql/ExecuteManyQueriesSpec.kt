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

package com.github.mauricio.async.db.mysql

import org.specs2.mutable.Specification

class ExecuteManyQueriesSpec extends Specification with ConnectionHelper {

  "connection" should {

    "execute many queries one after the other" in {

      withConnection {
        connection =>
          1.until(500).foreach {
            index =>
              val rows  = executeQuery(connection, "SELECT 6578, 'this is some text'").rows.get

              rows.size === 1

              val row = rows(0)

              row(0) === 6578
              row(1) === "this is some text"
          }

          success
      }

    }

    "execute many prepared statements one after the other" in {
      withConnection {
        connection =>
          1.until(500).foreach {
            index =>
              val rows  = executePreparedStatement(connection, "SELECT 6578, 'this is some text'").rows.get

              rows.size === 1

              val row = rows(0)

              row(0) === 6578
              row(1) === "this is some text"
          }

          success
      }
    }

  }

}
