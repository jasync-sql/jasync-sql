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

import java.sql.Timestamp
import java.util.concurrent.TimeUnit
import org.joda.time._
import org.specs2.mutable.Specification
import scala.concurrent.duration.Duration

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

    "be able to select numbers and process them" in {

      withConnection {
        connection =>
          executeQuery(connection, createTableNumericColumns)
          executeQuery(connection, insertTableNumericColumns)
          val result = executePreparedStatement(connection, "SELECT * FROM numbers").rows.get(0)

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

    "be able to select from a table with timestamps" in {

      withConnection {
        connection =>
          executeQuery(connection, createTableTimeColumns)
          executeQuery(connection, insertTableTimeColumns)
          val result = executePreparedStatement(connection, "SELECT * FROM posts").rows.get(0)

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

    "it should be able to bind statement values to the prepared statement" in {

      withConnection {
        connection =>
          val insert =
            """
              |insert into numbers (
              |number_tinyint,
              |number_smallint,
              |number_mediumint,
              |number_int,
              |number_bigint,
              |number_decimal,
              |number_float,
              |number_double
              |) values
              |(
              |?,
              |?,
              |?,
              |?,
              |?,
              |?,
              |?,
              |?)
            """.stripMargin


          val byte: Byte = 10
          val short: Short = 679
          val mediumInt = 778
          val int = 875468
          val bigInt = BigInt(100007654)
          val bigDecimal = BigDecimal("198.657809")
          val double = 98.765
          val float = 432.8F

          executeQuery(connection, this.createTableNumericColumns)
          executePreparedStatement(connection,
            insert,
            byte,
            short,
            mediumInt,
            int,
            bigInt,
            bigDecimal,
            float,
            double)

          val row = executePreparedStatement(connection, "SELECT * FROM numbers").rows.get(0)

          row("number_tinyint") === byte
          row("number_smallint") === short
          row("number_mediumint") === mediumInt
          row("number_int") === int
          row("number_bigint") === bigInt
          row("number_decimal") === bigDecimal
          row("number_float") === float
          row("number_double") === double

      }

    }

    "bind parameters on a prepared statement" in {

      val create = """CREATE TEMPORARY TABLE posts (
                     |       id INT NOT NULL AUTO_INCREMENT,
                     |       some_text TEXT not null,
                     |       primary key (id) )""".stripMargin

      val insert = "insert into posts (some_text) values (?)"
      val select = "select * from posts"

      withConnection {
        connection =>
          executeQuery(connection, create)
          executePreparedStatement(connection, insert, "this is some text here")
          val row = executePreparedStatement(connection, select).rows.get(0)

          row("id") === 1
          row("some_text") === "this is some text here"

          val queryRow = executeQuery(connection, select).rows.get(0)

          queryRow("id") === 1
          queryRow("some_text") === "this is some text here"


      }
    }

    "bind timestamp parameters to a table" in {

      val insert =
        """
          |insert into posts (created_at_date, created_at_datetime, created_at_timestamp, created_at_time, created_at_year)
          |values ( ?, ?, ?, ?, ? )
        """.stripMargin

      val date = new LocalDate(2011, 9, 8)
      val dateTime = new LocalDateTime(2012, 5, 27, 15, 29, 55)
      val timestamp = new Timestamp(dateTime.toDateTime.getMillis)
      val time = Duration( 3, TimeUnit.HOURS ) + Duration( 5, TimeUnit.MINUTES ) + Duration(10, TimeUnit.SECONDS)
      val year = 2012

      withConnection {
        connection =>
          executeQuery(connection, this.createTableTimeColumns)
          executePreparedStatement(connection, insert, date, dateTime, timestamp, time, year)
          val rows = executePreparedStatement(connection, "select * from posts where created_at_year > ?", 2011).rows.get

          rows.length === 1
          val row = rows(0)

          row("created_at_date") === date
          row("created_at_timestamp") === new LocalDateTime( timestamp.getTime )
          row("created_at_time") === time
          row("created_at_year") === year
          row("created_at_datetime") === dateTime

      }
    }

    "read a timestamp with microseconds" in {



      val create =
        """CREATE TEMPORARY TABLE posts (
       id INT NOT NULL AUTO_INCREMENT,
       created_at_timestamp TIMESTAMP(3) not null,
       created_at_time TIME(3) not null,
       primary key (id)
      )"""

      val insert =
        """INSERT INTO posts ( created_at_timestamp, created_at_time )
          | VALUES ( '2013-01-19 03:14:07.019', '03:14:07.019' )""".stripMargin

      val time = Duration(3, TimeUnit.HOURS ) +
        Duration(14, TimeUnit.MINUTES) +
        Duration(7, TimeUnit.SECONDS) +
        Duration(19, TimeUnit.MILLISECONDS)

      val timestamp = new LocalDateTime(2013, 1, 19, 3, 14, 7, 19)
      val select = "SELECT * FROM posts"

      withConnection {
        connection =>

          if ( connection.version < MySQLConnection.MicrosecondsVersion ) {
            pending(s"this version of MySQL (${connection.version}) does not support microseconds")
          } else {
            executeQuery(connection, create)
            executeQuery(connection, insert)
            val rows = executePreparedStatement( connection, select).rows.get

            val row = rows(0)

            row("created_at_time") === time
            row("created_at_timestamp") === timestamp

            val otherRow = executeQuery( connection, select ).rows.get(0)

            otherRow("created_at_time") === time
            otherRow("created_at_timestamp") === timestamp
          }

      }

    }

    "support prepared statement with a big string" in {

      val bigString = {
        val builder = new StringBuilder()
        for (i <- 0 until 400)
          builder.append( "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789")
        builder.toString()
      }

      withConnection {
        connection =>
          executeQuery(connection, "CREATE TEMPORARY TABLE BIGSTRING( id INT NOT NULL AUTO_INCREMENT, STRING LONGTEXT, primary key (id))")
          executePreparedStatement(connection, "INSERT INTO BIGSTRING (STRING) VALUES (?)", bigString)
          val row = executePreparedStatement(connection, "SELECT STRING, id FROM BIGSTRING").rows.get(0)
          row("id") === 1
          val result = row("STRING").asInstanceOf[String]
          result === bigString
      }
    }

    "support setting null to a column" in {
      withConnection {
        connection =>
          executeQuery(connection, "CREATE TEMPORARY TABLE timestamps ( id INT NOT NULL, moment TIMESTAMP NULL, primary key (id))")
          executePreparedStatement(connection, "INSERT INTO timestamps (moment, id) VALUES (?, ?)", null, 10)
          val row = executePreparedStatement(connection, "SELECT moment, id FROM timestamps").rows.get(0)
          row("id") === 10
          row("moment") === null
      }
    }

    "support setting None to a column" in {
      withConnection {
        connection =>
          executeQuery(connection, "CREATE TEMPORARY TABLE timestamps ( id INT NOT NULL, moment TIMESTAMP NULL, primary key (id))")
          executePreparedStatement(connection, "INSERT INTO timestamps (moment, id) VALUES (?, ?)", None, 10)
          val row = executePreparedStatement(connection, "SELECT moment, id FROM timestamps").rows.get(0)
          row("id") === 10
          row("moment") === null
      }
    }

    "support setting Some(value) to a column" in {
      withConnection {
        connection =>
          executeQuery(connection, "CREATE TEMPORARY TABLE timestamps ( id INT NOT NULL, moment TIMESTAMP NULL, primary key (id))")
          val moment = LocalDateTime.now().withMillisOfDay(0) // cut off millis to match timestamp
          executePreparedStatement(connection, "INSERT INTO timestamps (moment, id) VALUES (?, ?)", Some(moment), 10)
          val row = executePreparedStatement(connection, "SELECT moment, id FROM timestamps").rows.get(0)
          row("id") === 10
          row("moment") === moment
      }
    }

  }

}