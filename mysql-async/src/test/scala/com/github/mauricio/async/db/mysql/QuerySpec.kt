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

import com.github.mauricio.async.db.mysql.exceptions.MySQLException
import org.joda.time._
import org.specs2.mutable.Specification
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import io.netty.util.CharsetUtil
import com.github.mauricio.async.db.exceptions.InsufficientParametersException
import org.specs2.matcher.MatchResult
import com.github.mauricio.async.db.{QueryResult, ResultSet}

class QuerySpec extends Specification with ConnectionHelper {

  "connection" should {

    "be able to run a DML query" in {

      withConnection {
        connection =>
          executeQuery(connection, this.createTable).rowsAffected === 0
      }

    }

    "raise an exception upon a bad statement" in {
      withConnection {
        connection =>
          executeQuery(connection, "this is not SQL") must throwA[MySQLException].like {
            case e => e.asInstanceOf[MySQLException].errorMessage.sqlState === "#42000"
          }
      }
    }

    "be able to select from a table" in {

      withConnection {
        connection =>
          executeQuery(connection, this.createTable).rowsAffected === 0
          executeQuery(connection, this.insert).rowsAffected === 1
          val result = executeQuery(connection, this.select).rows.get

          result(0)("id") === 1
          result(0)("name") === "Maurício Aragão"
      }

    }

    "be able to select from a table with timestamps" in {

      withConnection {
        connection =>
          executeQuery(connection, createTableTimeColumns)
          executeQuery(connection, insertTableTimeColumns)
          val result = executeQuery(connection, "SELECT * FROM posts").rows.get(0)

          val date = result("created_at_date").asInstanceOf[LocalDate]

          date.getYear === 2038
          date.getMonthOfYear === 1
          date.getDayOfMonth === 19

          val dateTime = result("created_at_datetime").asInstanceOf[LocalDateTime]
          dateTime.getYear === 2013
          dateTime.getMonthOfYear === 1
          dateTime.getDayOfMonth === 19
          dateTime.getHourOfDay === 3
          dateTime.getMinuteOfHour === 14
          dateTime.getSecondOfMinute === 7

          val timestamp = result("created_at_timestamp").asInstanceOf[LocalDateTime]
          timestamp.getYear === 2020
          timestamp.getMonthOfYear === 1
          timestamp.getDayOfMonth === 19
          timestamp.getHourOfDay === 3
          timestamp.getMinuteOfHour === 14
          timestamp.getSecondOfMinute === 7


          result("created_at_time") === Duration(3, TimeUnit.HOURS) + Duration(14, TimeUnit.MINUTES) + Duration(7, TimeUnit.SECONDS)

          val year = result("created_at_year").asInstanceOf[Short]

          year === 1999
      }

    }

    "be able to select from a table with the various numeric types" in {

      withConnection {
        connection =>
          executeQuery(connection, createTableNumericColumns)
          executeQuery(connection, insertTableNumericColumns)
          val result = executeQuery(connection, "SELECT * FROM numbers").rows.get(0)

          result("number_tinyint").asInstanceOf[Byte] === -100
          result("number_smallint").asInstanceOf[Short] === 32766
          result("number_mediumint").asInstanceOf[Int] === 8388607
          result("number_int").asInstanceOf[Int] === 2147483647
          result("number_bigint").asInstanceOf[Long] === 9223372036854775807L
          result("number_decimal") === BigDecimal(450.764491)
          result("number_float") === 14.7F
          result("number_double") === 87650.9876
      }

    }

    "be able to read from a BLOB column when in text protocol" in {
      val create = """CREATE TEMPORARY TABLE posts (
                     |       id INT NOT NULL AUTO_INCREMENT,
                     |       some_bytes BLOB not null,
                     |       primary key (id) )""".stripMargin

      val insert = "insert into posts (some_bytes) values (?)"
      val select = "select * from posts"
      val bytes = "this is some text here".getBytes(CharsetUtil.UTF_8)

      withConnection {
        connection =>
          executeQuery(connection, create)
          executePreparedStatement(connection, insert, bytes)
          val row = executeQuery(connection, select).rows.get(0)
          row("id") === 1
          row("some_bytes") === bytes
      }
    }

    "have column names on result set" in {

      val create = """CREATE TEMPORARY TABLE posts (
                     |       id INT NOT NULL AUTO_INCREMENT,
                     |       some_bytes BLOB not null,
                     |       primary key (id) )""".stripMargin

      val createIdeas = """CREATE TEMPORARY TABLE ideas (
                          |       id INT NOT NULL AUTO_INCREMENT,
                          |       some_idea VARCHAR(255) NOT NULL,
                          |       primary key (id) )""".stripMargin

      val select = "SELECT * FROM posts"
      val selectIdeas = "SELECT * FROM ideas"

      val matcher: QueryResult => List[MatchResult[IndexedSeq[String]]] = { result =>
        val columns = result.rows.get.columnNames
        List(columns must contain(allOf("id", "some_bytes")).inOrder, columns must have size (2))
      }

      val ideasMatcher: QueryResult => List[MatchResult[IndexedSeq[String]]] = { result =>
        val columns = result.rows.get.columnNames
        List(columns must contain(allOf("id", "some_idea")).inOrder, columns must have size (2))
      }

      withConnection {
        connection =>
          executeQuery(connection, create)
          executeQuery(connection, createIdeas)

          matcher(executePreparedStatement(connection, select))
          ideasMatcher(executePreparedStatement(connection, selectIdeas))

          matcher(executePreparedStatement(connection, select))
          ideasMatcher(executePreparedStatement(connection, selectIdeas))

          matcher(executeQuery(connection, select))
          ideasMatcher(executeQuery(connection, selectIdeas))

          success("completed")
      }

    }

    "support BIT type" in {

      val create =
        """CREATE TEMPORARY TABLE POSTS (
          | id INT NOT NULL AUTO_INCREMENT,
          | bit_column BIT(20),
          | primary key (id))
        """.stripMargin

      val insert = "INSERT INTO POSTS (bit_column) VALUES (b'10000000')"
      val select = "SELECT * FROM POSTS"

      withConnection {
        connection =>
          executeQuery(connection, create)
          executeQuery(connection, insert)

          val rows = executeQuery(connection, select).rows.get
          rows(0)("bit_column") === Array(0, 0, -128)

          val preparedRows = executePreparedStatement(connection, select).rows.get
          preparedRows(0)("bit_column") === Array(0, 0, -128)
      }

    }

    "fail if number of args required is different than the number of provided parameters" in {

      withConnection {
        connection =>
          executePreparedStatement(
            connection,
            "select * from some_table where c = ? and b = ?",
            "one", "two", "three") must throwAn[InsufficientParametersException]
      }

    }

    "select from another empty table with many columns" in {
      withConnection {
        connection =>
          val create = """create temporary table test_11 (
                         |    id int primary key not null,
                         |    c2 text not null, c3 text not null, c4 text not null,
                         |    c5 text not null, c6 text not null, c7 text not null,
                         |    c8 text not null, c9 text not null, c10 text not null,
                         |    c11 text not null
                         |) ENGINE=InnoDB DEFAULT CHARSET=utf8;""".stripMargin

          executeQuery(connection, create)

          val result = executeQuery(connection, "select * from test_11")

          result.rows.get.size === 0
      }
    }

    "select from an empty table with many columns" in {

      withConnection {
        connection =>

          val create = """create temporary table test_10 (
                         |    id int primary key not null,
                         |    c2 text not null, c3 text not null, c4 text not null,
                         |    c5 text not null, c6 text not null, c7 text not null,
                         |    c8 text not null, c9 text not null, c10 text not null
                         |) ENGINE=InnoDB DEFAULT CHARSET=utf8;""".stripMargin

          executeQuery(connection, create)

          val result = executeQuery(connection, "select * from test_10")

          result.rows.get.size === 0
      }

    }

    "select from a large text column" in {

      val create = "create temporary table bombs (id char(4), bomb mediumtext character set ascii)"

      val insert = """  insert bombs values
                     |  ('bomb', repeat(' ',65536+16384+8192+4096+2048+1024+512+256+128)),
                     |  ('good', repeat(' ',65536+16384+8192+4096+2048+1024+512+256+128-1))""".stripMargin


      withConnection {
        connection =>
          executeQuery(connection, create)
          executeQuery(connection, insert)
          val result = executeQuery(connection, "select bomb from bombs").rows.get

          result.size === 2

          result(0)("bomb").asInstanceOf[String].length === 98176
          result(1)("bomb").asInstanceOf[String].length === 98175
      }

    }


  }

}
