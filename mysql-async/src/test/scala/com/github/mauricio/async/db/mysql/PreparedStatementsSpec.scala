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
import org.joda.time._
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit

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

          result("created_at_time") === Duration( 3, TimeUnit.HOURS ) + Duration( 14, TimeUnit.MINUTES ) + Duration( 7, TimeUnit.SECONDS )

          val year = result("created_at_year").asInstanceOf[Short]

          year === 1999
      }

    }

  }

}
