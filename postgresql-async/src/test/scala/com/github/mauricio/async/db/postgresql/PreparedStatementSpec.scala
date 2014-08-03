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
import org.joda.time.LocalDate
import com.github.mauricio.async.db.util.Log
import com.github.mauricio.async.db.exceptions.InsufficientParametersException
import java.util.Date
import com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException

class PreparedStatementSpec extends Specification with DatabaseTestHelper {

  val log = Log.get[PreparedStatementSpec]

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
  val messagesSelectEscaped = "SELECT id, content, moment FROM messages WHERE content LIKE '%??%' AND id > ?"

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

    "run two different prepared statements in sequence and get the right values" in {

      val create = """CREATE TEMP TABLE other_messages
                         (
                           id bigserial NOT NULL,
                           other_moment date NULL,
                           other_content character varying(255) NOT NULL,
                           CONSTRAINT other_messages_bigserial_column_pkey PRIMARY KEY (id )
                         )"""

      val select = "SELECT * FROM other_messages"
      val insert = "INSERT INTO other_messages (other_moment, other_content) VALUES (?, ?)"

      val moment = LocalDate.now()
      val otherMoment = LocalDate.now().minusDays(10)

      val message = "this is some message"
      val otherMessage = "this is some other message"

      withHandler {
        handler =>
          executeDdl(handler, this.messagesCreate)
          executeDdl(handler, create)

          foreach(1.until(4)) {
            x =>
              executePreparedStatement(handler, this.messagesInsert, Array(message, moment))
              executePreparedStatement(handler, insert, Array(otherMoment, otherMessage))

              val result = executePreparedStatement(handler, this.messagesSelectAll).rows.get
              result.size === x
              result.columnNames must contain(allOf("id", "content", "moment")).inOrder
              result(x - 1)("moment") === moment
              result(x - 1)("content") === message

              val otherResult = executePreparedStatement(handler, select).rows.get
              otherResult.size === x
              otherResult.columnNames must contain(allOf("id", "other_moment", "other_content")).inOrder
              otherResult(x - 1)("other_moment") === otherMoment
              otherResult(x - 1)("other_content") === otherMessage
          }

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

    "support prepared statement with escaped placeholders" in {
      withHandler {
        handler =>

          val firstContent = "Some? Moment"
          val secondContent = "Some Other Moment"
          val date = LocalDate.now()

          executeDdl(handler, this.messagesCreate)
          executePreparedStatement(handler, this.messagesInsert, Array(Some(firstContent), None))
          executePreparedStatement(handler, this.messagesInsert, Array(Some(secondContent), Some(date)))

          val rows = executePreparedStatement(handler, this.messagesSelectEscaped, Array(0)).rows.get

          rows.length === 1

          rows(0)("id") === 1
          rows(0)("content") === firstContent
          rows(0)("moment") === null

      }
    }

    "support handling of enum types" in {

      withHandler {
        handler =>
          val create = """CREATE TEMP TABLE messages
                         |(
                         |id bigserial NOT NULL,
                         |feeling example_mood,
                         |CONSTRAINT bigserial_column_pkey PRIMARY KEY (id )
                         |);""".stripMargin
          val insert = "INSERT INTO messages (feeling) VALUES (?) RETURNING id"
          val select = "SELECT * FROM messages"

          executeDdl(handler, create)

          executePreparedStatement(handler, insert, Array("sad"))

          val result = executePreparedStatement(handler, select).rows.get

          result.size === 1
          result(0)("id") === 1L
          result(0)("feeling") === "sad"
      }

    }

    "support handling JSON type" in {

      if ( System.getenv("TRAVIS") == null ) {
        withHandler {
          handler =>
            val create = """create temp table people
                           |(
                           |id bigserial primary key,
                           |addresses json,
                           |phones json
                           |);""".stripMargin

            val insert = "INSERT INTO people (addresses, phones) VALUES (?,?) RETURNING id"
            val select = "SELECT * FROM people"
            val addresses = """[ {"Home" : {"city" : "Tahoe", "state" : "CA"}} ]"""
            val phones = """[ "925-575-0415", "916-321-2233" ]"""

            executeDdl(handler, create)
            executePreparedStatement(handler, insert, Array(addresses, phones) )
            val result = executePreparedStatement(handler, select).rows.get

            result(0)("addresses") === addresses
            result(0)("phones") === phones
        }
        success
      } else {
        pending
      }
    }

    "support select bind value" in {
      withHandler {
        handler =>
          val string = "someString"
          val result = executePreparedStatement(handler, "SELECT CAST(? AS VARCHAR)", Array(string)).rows.get
          result(0)(0) === string
      }
    }

    "fail if prepared statement has more variables than it was given" in {
      withHandler {
        handler =>
          executeDdl(handler, messagesCreate)

          handler.sendPreparedStatement(
            "SELECT * FROM messages WHERE content = ? AND moment = ?",
            Array("some content")) must throwAn[InsufficientParametersException]
      }
    }

    "run prepared statement twice with bad and good values" in {
      withHandler {
        handler =>
          val content = "Some Moment"

          val query = "SELECT content FROM messages WHERE id = ?"

          executeDdl(handler, messagesCreate)
          executePreparedStatement(handler, this.messagesInsert, Array(Some(content), None))

          executePreparedStatement(handler, query, Array("undefined")) must throwA[GenericDatabaseException]
          val result = executePreparedStatement(handler, query, Array(1)).rows.get
          result(0)(0) === content
      }
    }

  }

}
