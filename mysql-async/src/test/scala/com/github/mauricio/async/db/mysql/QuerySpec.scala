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
import org.jboss.netty.util.CharsetUtil

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


          result("created_at_time") === Duration( 3, TimeUnit.HOURS ) + Duration( 14, TimeUnit.MINUTES ) + Duration( 7, TimeUnit.SECONDS )

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

  }

}
