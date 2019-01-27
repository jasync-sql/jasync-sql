package com.github.jasync.sql.db.mysql

import com.github.jasync.sql.db.invoke
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime
import org.junit.Test
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PreparedStatementsSpec : ConnectionHelper() {

    @Test
    fun `be able to execute prepared statements`() {

        withConnection { connection ->
            val result = assertNotNull(executePreparedStatement(connection, "select 1 as id , 'joe' as name").rows)
            assertEquals(1, result.size)
            assertEquals("joe", result[0]["name"])
            assertEquals(1L, result[0]["id"])
            val otherResult = assertNotNull(executePreparedStatement(connection, "select 1 as id , 'joe' as name").rows)

            assertEquals(1, otherResult.size)
            assertEquals("joe", otherResult[0]["name"])
            assertEquals(1L, otherResult[0]["id"])
        }
    }

    @Test
    fun `be able to detect a null value()a prepared statement`() {

        withConnection { connection ->
            val result = assertNotNull(
                executePreparedStatement(
                    connection,
                    "select 1 as id , 'joe' as name, NULL as null_value"
                ).rows
            )
            assertEquals(1, result.size)
            assertEquals("joe", result[0]["name"])
            assertEquals(1L, result[0]["id"])
            assertNull(result[0]["null_value"])
        }

    }

    @Test
    fun `be able to select numbers and process them`() {

        withConnection { connection ->
            executeQuery(connection, createTableNumericColumns)
            executeQuery(connection, insertTableNumericColumns)
            val result =
                assertNotNull(assertNotNull(executePreparedStatement(connection, "SELECT * FROM numbers").rows)[0])

            assertEquals(-100, result["number_tinyint"] as Byte)
            assertEquals(32766, result["number_smallint"] as Short)
            assertEquals(8388607, result["number_mediumint"] as Int)
            assertEquals(2147483647, result["number_int"] as Int)
            assertEquals(9223372036854775807, result["number_bigint"] as Long)
            assertEquals(BigDecimal(450.764491).toDouble(), (result["number_decimal"] as BigDecimal).toDouble())
            assertEquals(14.7F, result["number_float"])
            assertEquals(87650.9876, result["number_double"])
        }

    }

    @Test
    fun `be able to select from a table with timestamps`() {

        withConnection { connection ->
            executeQuery(connection, createTableTimeColumns)
            executeQuery(connection, insertTableTimeColumns)
            val result =
                assertNotNull(assertNotNull(executePreparedStatement(connection, "SELECT * FROM posts").rows)[0])
            val date = result["created_at_date"] as org.joda.time.LocalDate

            assertEquals(2038, date.year)
            assertEquals(1, date.monthOfYear)
            assertEquals(19, date.dayOfMonth)

            val dateTime = result["created_at_datetime"] as org.joda.time.LocalDateTime

            assertEquals(2013, dateTime.year)
            assertEquals(1, dateTime.monthOfYear)
            assertEquals(19, dateTime.dayOfMonth)
            assertEquals(3, dateTime.hourOfDay)
            assertEquals(14, dateTime.minuteOfHour)
            assertEquals(7, dateTime.secondOfMinute)

            val timestamp = result["created_at_timestamp"] as org.joda.time.LocalDateTime

            assertEquals(2020, timestamp.year)
            assertEquals(1, timestamp.monthOfYear)
            assertEquals(19, timestamp.dayOfMonth)
            assertEquals(3, timestamp.hourOfDay)
            assertEquals(14, timestamp.minuteOfHour)
            assertEquals(7, timestamp.secondOfMinute)

            assertEquals(
                Duration.ofHours(3).plus(Duration.ofMinutes(14).plus(Duration.ofSeconds(7))),
                result["created_at_time"]
            )

            val year = result["created_at_year"] as Short
            assertEquals(1999, year)
        }

    }

    @Test
    fun `it should be able to bind statement values to the prepared statement`() {

        withConnection { connection ->
            val insert =
                """
              insert into numbers (
              number_tinyint,
              number_smallint,
              number_mediumint,
              number_int,
              number_bigint,
              number_decimal,
              number_float,
              number_double
              ) values
              (
              ?,
              ?,
              ?,
              ?,
              ?,
              ?,
              ?,
              ?)
            """


            val byte: Byte = 10
            val short: Short = 679
            val mediumInt = 778
            val int = 875468
            val bigInt = 100007654L
            val bigDecimal = BigDecimal("198.657809")
            val double = 98.765
            val float = 432.8F

            executeQuery(connection, this.createTableNumericColumns)
            executePreparedStatement(
                connection,
                insert,
                listOf(
                    byte,
                    short,
                    mediumInt,
                    int,
                    bigInt,
                    bigDecimal,
                    float,
                    double
                )
            )

            val row = assertNotNull(executePreparedStatement(connection, "SELECT * FROM numbers").rows)[0]

            assertEquals(byte, row["number_tinyint"])
            assertEquals(short, row["number_smallint"])
            assertEquals(mediumInt, row["number_mediumint"])
            assertEquals(int, row["number_int"])
            assertEquals(bigInt, row["number_bigint"])
            assertEquals(bigDecimal, row["number_decimal"])
            assertEquals(float, row["number_float"])
            assertEquals(double, row["number_double"])
        }

    }

    @Test
    fun `bind parameters on a prepared statement`() {

        val create = """CREATE TEMPORARY TABLE posts (
                            id INT NOT NULL AUTO_INCREMENT,
                            some_text TEXT not null,
                            primary key (id) )"""

        val insert = "insert into posts (some_text) values (?)"
        val select = "select * from posts"

        withConnection { connection ->
            executeQuery(connection, create)
            executePreparedStatement(connection, insert, listOf("this is some text here"))
            val row = assertNotNull(executePreparedStatement(connection, select).rows)[0]

            assertEquals(1, row["id"])
            assertEquals("this is some text here", row["some_text"])

            val queryRow = assertNotNull(executeQuery(connection, select).rows)[0]

            assertEquals(1, queryRow["id"])
            assertEquals("this is some text here", queryRow["some_text"])


        }
    }

    @Test
    fun `bind timestamp parameters to a table`() {

        val insert =
            """
          insert into posts (created_at_date, created_at_datetime, created_at_timestamp, created_at_time, created_at_year)
          values ( ?, ?, ?, ?, ? )
        """

        val date = LocalDate(2011, 9, 8)
        val dateTime = LocalDateTime(2012, 5, 27, 15, 29, 55)
        val timestamp = Timestamp(dateTime.toDateTime().millis)
        val time = Duration.ofHours(3) + Duration.ofMinutes(5) + Duration.ofSeconds(10)
        val year = 2012.toShort()

        withConnection { connection ->
            executeQuery(connection, this.createTableTimeColumns)
            executePreparedStatement(connection, insert, listOf(date, dateTime, timestamp, time, year))
            val rows = assertNotNull(
                executePreparedStatement(
                    connection,
                    "select * from posts where created_at_year > ?",
                    listOf(2011)
                ).rows
            )

            assertEquals(1, rows.size)
            val row = assertNotNull(rows[0])

            assertEquals(date, row["created_at_date"])
            assertEquals(LocalDateTime(timestamp.time), row["created_at_timestamp"])
            assertEquals(time, row["created_at_time"])
            assertEquals(year, row["created_at_year"])
            assertEquals(dateTime, row["created_at_datetime"])

        }
    }

    @Test
    fun `read a timestamp with microseconds`() {

        val create =
            """CREATE TEMPORARY TABLE posts (
       id INT NOT NULL AUTO_INCREMENT,
       created_at_timestamp TIMESTAMP(3) not null,
       created_at_time TIME(3) not null,
       primary key (id)
      )"""

        val insert =
            """INSERT INTO posts ( created_at_timestamp, created_at_time )
           VALUES ( '2013-01-19 03:14:07.019', '03:14:07.019' )"""

        val time = Duration.ofHours(3) +
                Duration.ofMinutes(14) +
                Duration.ofSeconds(7) +
                Duration.ofMillis(19)

        val timestamp = LocalDateTime(2013, 1, 19, 3, 14, 7, 19)
        val select = "SELECT * FROM posts"

        withConnection { connection ->
            executeQuery(connection, create)
            executeQuery(connection, insert)
            val rows = assertNotNull(executePreparedStatement(connection, select).rows)

            val row = assertNotNull(rows[0])

            assertEquals(time, row["created_at_time"])
            assertEquals(timestamp, row["created_at_timestamp"])

            val otherRow = assertNotNull(executeQuery(connection, select).rows)[0]

            assertEquals(time, otherRow["created_at_time"])
            assertEquals(timestamp, otherRow["created_at_timestamp"])
        }
    }

    @Test
    fun `support prepared statement with a big string`() {

        val bigString: String
        val builder = StringBuilder()
        (0..400).map { builder.append("0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789") }

        bigString = builder.toString()


        withConnection { connection ->
            executeQuery(
                connection,
                "CREATE TEMPORARY TABLE BIGSTRING( id INT NOT NULL AUTO_INCREMENT, STRING LONGTEXT, primary key (id))"
            )
            executePreparedStatement(connection, "INSERT INTO BIGSTRING (STRING) VALUES (?)", listOf(bigString))
            val row = assertNotNull(executePreparedStatement(connection, "SELECT STRING, id FROM BIGSTRING").rows)[0]
            assertEquals(1, row["id"])
            val result = row["STRING"] as String
            assertEquals(bigString, result)
        }
    }

    @Test
    fun `support setting null to a column`() {
        val somethingNull: String? = null
        withConnection { connection ->
            executeQuery(
                connection,
                "CREATE TEMPORARY TABLE timestamps ( id INT NOT NULL, moment TIMESTAMP NULL, primary key (id))"
            )
            executePreparedStatement(
                connection,
                "INSERT INTO timestamps (moment, id) VALUES (?, ?)",
                listOf(somethingNull, 10)
            )
            val row = assertNotNull(executePreparedStatement(connection, "SELECT moment, id FROM timestamps").rows)[0]
            assertEquals(10, row["id"])
            assertNull(row["moment"])
        }
    }

    @Test
    fun `support setting None to a column`() {
        val someNull: String? = null
        withConnection { connection ->
            executeQuery(
                connection,
                "CREATE TEMPORARY TABLE timestamps ( id INT NOT NULL, moment TIMESTAMP NULL, primary key (id))"
            )
            executePreparedStatement(
                connection,
                "INSERT INTO timestamps (moment, id) VALUES (?, ?)",
                listOf(someNull, 10)
            )
            val row = assertNotNull(executePreparedStatement(connection, "SELECT moment, id FROM timestamps").rows)[0]
            assertEquals(10, row["id"])
            assertNull(row["moment"])
        }
    }

    @Test
    fun `support setting Some(value) to a column`() {
        withConnection { connection ->
            executeQuery(
                connection,
                "CREATE TEMPORARY TABLE timestamps ( id INT NOT NULL, moment TIMESTAMP NULL, primary key (id))"
            )
            val moment = LocalDateTime.now().withMillisOfDay(0) // cut off millis to match timestamp
            executePreparedStatement(
                connection,
                "INSERT INTO timestamps (moment, id) VALUES (?, ?)",
                listOf(moment, 10)
            )
            val row = assertNotNull(executePreparedStatement(connection, "SELECT moment, id FROM timestamps").rows)[0]
            assertEquals(10, row["id"])
            assertEquals(moment, row["moment"])
        }
    }

    @Test
    fun `bind parameters on a prepared statement with limit`() {

        val create = """CREATE TEMPORARY TABLE posts (
                            id INT NOT NULL AUTO_INCREMENT,
                            some_text TEXT not null,
                            some_date DATE,
                            primary key (id) )"""

        val insert = "insert into posts (some_text) values (?)"
        val select = "select * from posts limit 100"

        withConnection { connection ->
            executeQuery(connection, create)

            executePreparedStatement(connection, insert, listOf("this is some text here"))

            val row = assertNotNull(executeQuery(connection, select).rows)[0]

            assertEquals(1, row["id"])
            assertEquals("this is some text here", row["some_text"])
            assertNull(row["some_date"])

            val queryRow = assertNotNull(executePreparedStatement(connection, select).rows)[0]

            assertEquals(1, queryRow["id"])
            assertEquals("this is some text here", queryRow["some_text"])
            assertNull(queryRow["some_date"])

        }
    }

    @Test
    fun `insert with prepared statements and without columns`() {
        withConnection { connection ->
            executeQuery(connection, this.createTable)

            executePreparedStatement(connection, this.insert)

            val result = assertNotNull(executePreparedStatement(connection, this.select).rows)
            assertEquals(1, result.size)
            assertEquals("Boogie Man", result[0]["name"])
        }
    }

    @Test
    fun `be able to release prepared statements`() {
        withConnection { connection ->
            val query = "select 1 as id , 'joe' as name"
            val result = executePreparedStatement(connection, query).rows

            assertThat(result[0]("name")).isEqualTo("joe")
            assertThat(result(0)("id")).isEqualTo(1L)
            assertThat(result.size).isEqualTo(1)

            validateCounters(connection, prepare = 1, close = 0)

            val result2 = executePreparedStatement(connection, query).rows

            assertThat(result2[0]("name")).isEqualTo("joe")
            assertThat(result2(0)("id")).isEqualTo(1L)
            assertThat(result2.size).isEqualTo(1)

            validateCounters(connection, prepare = 1, close = 0)

            releasePreparedStatement(connection, query)

            Thread.sleep(2000)

            validateCounters(connection, prepare = 1, close = 1)

        }

    }

    private fun validateCounters(connection: MySQLConnection, prepare: Int, close: Int) {
        val statementMetrics = executeQuery(connection, "SHOW SESSION STATUS LIKE 'Com_stmt%'").rows
        assertThat(statementMetrics(3)("Variable_name")).isEqualTo("Com_stmt_prepare")
        assertThat(statementMetrics(3)("Value")).isEqualTo(prepare.toString())
        assertThat(statementMetrics(1)("Variable_name")).isEqualTo("Com_stmt_close")
        assertThat(statementMetrics(1)("Value")).isEqualTo(close.toString())
    }
}
