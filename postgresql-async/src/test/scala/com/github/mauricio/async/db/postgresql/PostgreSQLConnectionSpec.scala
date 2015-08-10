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

import java.nio.ByteBuffer

import com.github.mauricio.async.db.column.{DateEncoderDecoder, TimeEncoderDecoder, TimestampEncoderDecoder}
import com.github.mauricio.async.db.exceptions.UnsupportedAuthenticationMethodException
import com.github.mauricio.async.db.postgresql.exceptions.{GenericDatabaseException, QueryMustNotBeNullOrEmptyException}
import com.github.mauricio.async.db.postgresql.messages.backend.InformationMessage
import com.github.mauricio.async.db.util.Log
import com.github.mauricio.async.db.{Configuration, Connection, QueryResult}
import io.netty.buffer.Unpooled
import org.joda.time.LocalDateTime
import org.specs2.mutable.Specification

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object PostgreSQLConnectionSpec {
  val log = Log.get[PostgreSQLConnectionSpec]
}

class PostgreSQLConnectionSpec extends Specification with DatabaseTestHelper {

  import PostgreSQLConnectionSpec.log

  final val sampleArray = Array[Byte](83, 97, 121, 32, 72, 101, 108, 108, 111, 32, 116, 111, 32, 77, 121, 32, 76, 105, 116, 116, 108, 101, 32, 70, 114, 105, 101, 110, 100)

  val create = """create temp table type_test_table (
            bigserial_column bigserial not null,
            smallint_column smallint not null,
            integer_column integer not null,
            decimal_column decimal(10,4),
            real_column real,
            double_column double precision,
            serial_column serial not null,
            varchar_column varchar(255),
            text_column text,
            timestamp_column timestamp,
            date_column date,
            time_column time,
            boolean_column boolean,
            constraint bigserial_column_pkey primary key (bigserial_column)
          ) with oids"""

  val insert = """insert into type_test_table (
            smallint_column,
            integer_column,
            decimal_column,
            real_column,
            double_column,
            varchar_column,
            text_column,
            timestamp_column,
            date_column,
            time_column,
            boolean_column
            )
            VALUES (
            10,
            11,
            14.999,
            78.34,
            15.68,
            'this is a varchar field',
            'this is a long text field',
            '1984-08-06 22:13:45.888888',
            '1984-08-06',
            '22:13:45.888888',
            TRUE
            )
               """

  val select = "select *, oid from type_test_table"

  val preparedStatementCreate = """create temp table prepared_statement_test (
    id bigserial not null,
    name varchar(255) not null,
    constraint bigserial_column_pkey primary key (id)
  )"""

  val preparedStatementInsert = " insert into prepared_statement_test (name) values ('John Doe')"
  val preparedStatementInsert2 = " insert into prepared_statement_test (name) values ('Mary Jane')"
  val preparedStatementInsert3 = " insert into prepared_statement_test (name) values ('Peter Parker')"
  val preparedStatementInsertReturning = " insert into prepared_statement_test (name) values ('John Doe') returning id"
  val preparedStatementSelect = "select * from prepared_statement_test"

  "handler" should {

    "connect to the database" in {

      withHandler {
        handler =>
          handler.isReadyForQuery must beTrue
      }

    }

    "create a table in the database" in {

      withHandler {
        handler =>
          executeDdl(handler, this.create) === 0
      }

    }

    "insert a row in the database" in {

      withHandler {
        handler =>
          executeDdl(handler, this.create)
          executeDdl(handler, this.insert, 1) === 1

      }

    }

    "select rows in the database" in {

      withHandler {
        handler =>
          executeDdl(handler, this.create)
          executeDdl(handler, this.insert, 1)
          val result = executeQuery(handler, this.select)

          val row = result.rows.get(0)

          row(0) === 1
          row(1) === 10
          row(2) === 11
          row(3).toString === "14.9990"
          row(4).toString === 78.34.toString
          row(5) === 15.68
          row(6) === 1
          row(7) === "this is a varchar field"
          row(8) === "this is a long text field"
          row(9) === TimestampEncoderDecoder.Instance.decode("1984-08-06 22:13:45.888888")
          row(10) === DateEncoderDecoder.decode("1984-08-06")
          row(11) === TimeEncoderDecoder.Instance.decode("22:13:45.888888")
          row(12) === true
          row(13) must beAnInstanceOf[java.lang.Long]
          row(13).asInstanceOf[Long] must beGreaterThan(0L)


      }

    }

    "select rows that has duplicate column names" in {

      withHandler {
        handler =>
          val result = executeQuery(handler, "SELECT 1 COL, 2 COL")

          val row = result.rows.get(0)

          row(0) === 1
          row(1) === 2

      }

    }

    "execute a prepared statement" in {

      withHandler {
        handler =>
          executeDdl(handler, this.preparedStatementCreate)
          executeDdl(handler, this.preparedStatementInsert, 1)
          val result = executePreparedStatement(handler, this.preparedStatementSelect)

          val row = result.rows.get(0)


          row(0) === 1
          row(1) === "John Doe"


      }

    }

    "execute a prepared statement with parameters" in {

      withHandler {
        handler =>
          executeDdl(handler, this.preparedStatementCreate)
          executeDdl(handler, this.preparedStatementInsert, 1)
          executeDdl(handler, this.preparedStatementInsert2, 1)
          executeDdl(handler, this.preparedStatementInsert3, 1)

          val select = "select * from prepared_statement_test where name like ?"

          val queryResult = executePreparedStatement(handler, select, Array("Peter Parker"))
          val row = queryResult.rows.get(0)

          val queryResult2 = executePreparedStatement(handler, select, Array("Mary Jane"))
          val row2 = queryResult2.rows.get(0)

          row(0) === 3
          row(1) === "Peter Parker"

          row2(0) === 2
          row2(1) === "Mary Jane"

      }

    }

    "login using MD5 authentication" in {

      val configuration = new Configuration(
        username = "postgres_md5",
        password = Some("postgres_md5"),
        port = databasePort,
        database = databaseName
      )

      withHandler(configuration, {
        handler =>
          val result = executeQuery(handler, "SELECT 0")
          result.rows.get.apply(0)(0) === 0
      })

    }

    "login using cleartext authentication" in {

      val configuration = new Configuration(
        username = "postgres_cleartext",
        password = Some("postgres_cleartext"),
        port = databasePort,
        database = databaseName
      )

      withHandler(configuration, {
        handler =>
          val result = executeQuery(handler, "SELECT 0")
          result.rows.get(0)(0) === 0
      })

    }

    "fail login using kerberos authentication" in {

      val configuration = new Configuration(
        username = "postgres_kerberos",
        password = Some("postgres_kerberos"),
        port = databasePort,
        database = databaseName
      )

      withHandler(configuration, {
        handler =>
          executeQuery(handler, "SELECT 0")
      }) must throwAn[UnsupportedAuthenticationMethodException]

    }

    "fail login using with an invalid credential exception" in {

      val configuration = new Configuration(
        username = "postgres_md5",
        password = Some("postgres_kerberos"),
        port = databasePort,
        database = databaseName
      )
      try {
        withHandler(configuration, {
          handler =>
            val result = executeQuery(handler, "SELECT 0")
            throw new IllegalStateException("should not have arrived")
        })
      } catch {
        case e: GenericDatabaseException =>
          e.errorMessage.fields(InformationMessage.Routine) === "auth_failed"
      }

    }

    "transaction and flatmap example" in {

      val handler: Connection = new PostgreSQLConnection(defaultConfiguration)
      val result: Future[QueryResult] = handler.connect
        .map(parameters => handler)
        .flatMap(connection => connection.sendQuery("BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ"))
        .flatMap(query => handler.sendQuery("SELECT 0"))
        .flatMap(query => handler.sendQuery("COMMIT").map(value => query))

      val queryResult: QueryResult = Await.result(result, Duration(5, SECONDS))

      queryResult.rows.get(0)(0) === 0

    }

    "use RETURNING in an insert statement" in {

      withHandler {
        connection =>
          executeDdl(connection, this.preparedStatementCreate)
          val result = executeQuery(connection, this.preparedStatementInsertReturning)
          result.rows.get(0)("id") === 1
      }

    }

    "execute a prepared statement with limit" in {

      withHandler {
        handler =>
          executeDdl(handler, this.preparedStatementCreate)
          executeDdl(handler, this.preparedStatementInsert, 1)
          executeDdl(handler, this.preparedStatementInsert2, 1)
          executeDdl(handler, this.preparedStatementInsert3, 1)

          val result = executePreparedStatement(handler, "select * from prepared_statement_test LIMIT 1").rows.get(0)

          result("name") === "John Doe"
      }

    }

    "execute an empty query" in {

      withHandler {
        handler =>
          executeQuery(handler, "").rows === None
      } must throwA[QueryMustNotBeNullOrEmptyException]

    }

    "execute an whitespace query" in {

      withHandler {
        handler =>
          executeQuery(handler, "   ").rows === None
      } must throwA[QueryMustNotBeNullOrEmptyException]

    }

    "execute multiple prepared statements" in {
      withHandler {
        handler =>
          executeDdl(handler, this.preparedStatementCreate)
          for (i <- 0 until 1000)
            executePreparedStatement(handler, this.preparedStatementInsert)
          ok
      }
    }

    "load data from a bytea column" in {

      val create = """create temp table file_samples (
        id bigserial not null,
        content bytea not null,
        constraint bigserial_column_pkey primary key (id)
      )"""

      val insert = "insert into file_samples (content) values ( E'\\\\x5361792048656c6c6f20746f204d79204c6974746c6520467269656e64' ) "
      val select = "select * from file_samples"

      withHandler {
        handler =>
          executeDdl(handler, create)
          executeQuery(handler, insert)
          val rows = executeQuery(handler, select).rows.get

          rows(0)("content").asInstanceOf[Array[Byte]] === sampleArray

      }

    }

    "send data to a bytea column" in {

      val create = """create temp table file_samples (
        id bigserial not null,
        content bytea not null,
        constraint bigserial_column_pkey primary key (id)
      )"""

      val insert = "insert into file_samples (content) values ( ? ) "
      val select = "select * from file_samples"

      withHandler {
        handler =>

          executeDdl(handler, create)
          log.debug("executed create")
          executePreparedStatement(handler, insert, Array( sampleArray ))
          executePreparedStatement(handler, insert, Array( ByteBuffer.wrap(sampleArray) ))
          executePreparedStatement(handler, insert, Array( Unpooled.copiedBuffer(sampleArray) ))
          log.debug("executed prepared statement")
          val rows = executeQuery(handler, select).rows.get

          rows(0)("content").asInstanceOf[Array[Byte]] === sampleArray
          rows(1)("content").asInstanceOf[Array[Byte]] === sampleArray
          rows(2)("content").asInstanceOf[Array[Byte]] === sampleArray
      }

    }

    "insert a LocalDateTime" in {

      withHandler {
        handler =>
          executePreparedStatement(handler, "CREATE TEMP TABLE test(t TIMESTAMP)")
          val date1 = new LocalDateTime
          executePreparedStatement(handler, "INSERT INTO test(t) VALUES(?)", Array(date1))
          val result = executePreparedStatement(handler, "SELECT t FROM test")
          val date2 = result.rows.get.head(0)
          date1 === date2
      }

    }

    "insert without return after select" in {

      withHandler {
        handler =>
          executeDdl(handler, this.preparedStatementCreate)
          executeDdl(handler, this.preparedStatementInsert, 1)
          executeDdl(handler, this.preparedStatementSelect, 1)
          val result = executeQuery(handler, this.preparedStatementInsert2)

          result.rows === None
      }

    }

  }

}
