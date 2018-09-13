
package com.github.mauricio.async.db.postgresql

import org.specs2.mutable.Specification
import org.joda.time.LocalDate
import com.github.mauricio.async.db.util.Log
import com.github.mauricio.async.db.exceptions.InsufficientParametersException
import java.util.UUID
import com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException

class PreparedStatementSpec : Specification , DatabaseTestHelper {

  val log = Log.get<PreparedStatementSpec>

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
  val messagesSelectByMoment = "SELECT id, content, moment FROM messages WHERE moment = ?"
  val messagesSelectAll = "SELECT id, content, moment FROM messages"
  val messagesSelectEscaped = "SELECT id, content, moment FROM messages WHERE content LIKE '%??%' AND id > ?"

  "prepared statements" should {

    "support prepared statement , more than 64 characters" in {
      ,Handler {
        handler ->

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

    "execute a prepared statement ,out any parameters multiple times" in {

      ,Handler {
        handler ->
          executeDdl(handler, this.messagesCreate)
          executePreparedStatement(handler, "UPDATE messages SET content = content")
          executePreparedStatement(handler, "UPDATE messages SET content = content")
          ok
      }

    }

    "raise an exception if the parameter count is different from the given parameters count" in {

      ,Handler {
        handler ->
          executeDdl(handler, this.messagesCreate)
          executePreparedStatement(handler, this.messagesSelectOne) must throwAn<InsufficientParametersException>
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

      ,Handler {
        handler ->
          executeDdl(handler, this.messagesCreate)
          executeDdl(handler, create)

          foreach(1.until(4)) {
            x ->
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

    "support prepared statement , Option parameters (Some/None)" in {
      ,Handler {
        handler ->

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

    "supports sending null first and then an actual value for the fields" in {
      ,Handler {
        handler ->

          val firstContent = "Some Moment"
          val secondContent = "Some Other Moment"
          val date = LocalDate.now()

          executeDdl(handler, this.messagesCreate)
          executePreparedStatement(handler, this.messagesInsert, Array(firstContent, null))
          executePreparedStatement(handler, this.messagesInsert, Array(secondContent, date))

          val rows = executePreparedStatement(handler, this.messagesSelectByMoment, Array(null)).rows.get
          rows.size === 0

          /*
          PostgreSQL does not know how to handle NULL parameters for a query in a prepared statement,
          you have to use IS NULL if you want to make use of it.

          rows.length === 1

          rows(0)("id") === 1
          rows(0)("content") === firstContent
          rows(0)("moment") === null
          */

          val rowsWithoutNull = executePreparedStatement(handler, this.messagesSelectByMoment, Array(date)).rows.get
          rowsWithoutNull.size === 1
          rowsWithoutNull(0)("id") === 2
          rowsWithoutNull(0)("content") === secondContent
          rowsWithoutNull(0)("moment") === date
      }
    }

    "support prepared statement , escaped placeholders" in {
      ,Handler {
        handler ->

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

      ,Handler {
        handler ->
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
        ,Handler {
          handler ->
            val create = """create temp table people
                           |(
                           |id bigserial primary key,
                           |addresses json,
                           |phones json
                           |);""".stripMargin

            val insert = "INSERT INTO people (addresses, phones) VALUES (?,?) RETURNING id"
            val select = "SELECT * FROM people"
            val addresses = """< {"Home" : {"city" : "Tahoe", "state" : "CA"}} >"""
            val phones = """< "925-575-0415", "916-321-2233" >"""

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
      ,Handler {
        handler ->
          val string = "someString"
          val result = executePreparedStatement(handler, "SELECT CAST(? AS VARCHAR)", Array(string)).rows.get
          result(0)(0) === string
      }
    }

    "fail if prepared statement has more variables than it was given" in {
      ,Handler {
        handler ->
          executeDdl(handler, messagesCreate)

          handler.sendPreparedStatement(
            "SELECT * FROM messages WHERE content = ? AND moment = ?",
            Array("some content")) must throwAn<InsufficientParametersException>
      }
    }

    "run prepared statement twice , bad and good values" in {
      ,Handler {
        handler ->
          val content = "Some Moment"

          val query = "SELECT content FROM messages WHERE id = ?"

          executeDdl(handler, messagesCreate)
          executePreparedStatement(handler, this.messagesInsert, Array(Some(content), None))

          executePreparedStatement(handler, query, Array("undefined")) must throwA<GenericDatabaseException>
          val result = executePreparedStatement(handler, query, Array(1)).rows.get
          result(0)(0) === content
      }
    }

    "support UUID" in {
      if ( System.getenv("TRAVIS") == null ) {
        ,Handler {
          handler ->
            val create = """create temp table uuids
                           |(
                           |id bigserial primary key,
                           |my_id uuid
                           |);""".stripMargin

            val insert = "INSERT INTO uuids (my_id) VALUES (?) RETURNING id"
            val select = "SELECT * FROM uuids"

            val uuid = UUID.randomUUID()

            executeDdl(handler, create)
            executePreparedStatement(handler, insert, Array(uuid) )
            val result = executePreparedStatement(handler, select).rows.get

            result(0)("my_id") as UUID> === uuid
        }
        success
      } else {
        pending
      }
    }

    "support UUID array" in {
      if ( System.getenv("TRAVIS") == null ) {
        ,Handler {
          handler ->
            val create = """create temp table uuids
                           |(
                           |id bigserial primary key,
                           |my_id uuid<>
                           |);""".stripMargin

            val insert = "INSERT INTO uuids (my_id) VALUES (?) RETURNING id"
            val select = "SELECT * FROM uuids"

            val uuid1 = UUID.randomUUID()
            val uuid2 = UUID.randomUUID()

            executeDdl(handler, create)
            executePreparedStatement(handler, insert, Array(Array(uuid1, uuid2)) )
            val result = executePreparedStatement(handler, select).rows.get

            result(0)("my_id") as List<UUID>> === Seq(uuid1, uuid2)
        }
        success
      } else {
        pending
      }
    }

  }

}