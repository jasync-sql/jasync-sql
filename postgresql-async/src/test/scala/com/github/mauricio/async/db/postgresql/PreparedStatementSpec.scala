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

import org.specs2.mutable.Specification
import com.github.mauricio.async.db.postgresql.exceptions.InsufficientParametersException
import org.joda.time.LocalDate

class PreparedStatementSpec extends Specification with DatabaseTestHelper {

  val filler = List.fill(64)(" ").mkString("")

  val messagesCreate = """CREATE TEMP TABLE messages
                         (
                           id bigserial NOT NULL,
                           content character varying(255) NOT NULL,
                           moment date NULL,
                           CONSTRAINT bigserial_column_pkey PRIMARY KEY (id )
                         )"""
  val messagesInsert = s"INSERT INTO messages $filler (content,moment) VALUES (?,?) RETURNING id"
  val messagesInsertReverted = s"INSERT INTO messages $filler (moment,content) VALUES (?,?) RETURNING id"
  val messagesUpdate = "UPDATE messages SET content = ?, moment = ? WHERE id = ?"
  val messagesSelectOne = "SELECT id, content, moment FROM messages WHERE id = ?"
  val messagesSelectAll = "SELECT id, content, moment FROM messages"

  "prepared statements" should {

    "support prepared statement with more than 64 characters" in {
      withHandler {
        handler =>

          val firstContent = "Some Moment"
          val secondContent = "Some Other Moment"
          val date = LocalDate.now()

          executeDdl(handler, this.messagesCreate)
          executePreparedStatement(handler, this.messagesInsert, Array(firstContent, date))
          executePreparedStatement(handler, this.messagesInsertReverted, Array(date, secondContent))

          val rows = executePreparedStatement(handler, this.messagesSelectAll).rows.get

          rows.length === 2

          rows(0)("id") === 1
          rows(0)("content") === firstContent
          rows(0)("moment") === date

          rows(1)("id") === 2
          rows(1)("content") === secondContent
          rows(1)("moment") === date

      }
    }

    "execute a prepared statement without any parameters multiple times" in {

      withHandler {
        handler =>
          executeDdl(handler, this.messagesCreate)
          executePreparedStatement(handler, "UPDATE messages SET content = content")
          executePreparedStatement(handler, "UPDATE messages SET content = content")
          ok
      }

    }

    "raise an exception if the parameter count is different from the given parameters count" in {

      withHandler {
        handler =>
          executeDdl(handler, this.messagesCreate)
          executePreparedStatement(handler, this.messagesSelectOne) must throwAn[InsufficientParametersException]
      }

    }

    "support prepared statement with Option parameters (Some/None)" in {
      withHandler {
        handler =>

          val firstContent = "Some Moment"
          val secondContent = "Some Other Moment"
          val date = LocalDate.now()

          executeDdl(handler, this.messagesCreate)
          executePreparedStatement(handler, this.messagesInsert, Array(Some(firstContent), None))
          executePreparedStatement(handler, this.messagesInsert, Array(Some(secondContent), Some(date)))

          val rows = executePreparedStatement(handler, this.messagesSelectAll).rows.get

          rows.length === 2

          rows(0)("id") === 1
          rows(0)("content") === firstContent
          rows(0)("moment") === null

          rows(1)("id") === 2
          rows(1)("content") === secondContent
          rows(1)("moment") === date

      }
    }

  }

}
