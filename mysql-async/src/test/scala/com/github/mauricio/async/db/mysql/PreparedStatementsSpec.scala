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

class PreparedStatementsSpec extends Specification with ConnectionHelper {

  "connection" should {

    "be able to execute prepared statements" in {

      withConnection {
        connection =>
          val result = executePreparedStatement(connection, "select 1 as id , 'joe' as name").rows.get

          result(0)("name") === "joe"
          result(0)("id") === 1
          result.length === 1

          val otherResult = executePreparedStatement(connection, "select 1 as id , 'joe' as name").rows.get

          otherResult(0)("name") === "joe"
          otherResult(0)("id") === 1
          otherResult.length === 1
      }

    }

    "be able to detect a null value in a prepared statement" in {

      withConnection {
        connection =>
          val result = executePreparedStatement(connection, "select 1 as id , 'joe' as name, NULL as null_value").rows.get

          result(0)("name") === "joe"
          result(0)("id") === 1
          result(0)("null_value") must beNull
          result.length === 1

      }

    }

  }

}
