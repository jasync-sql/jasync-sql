package com.github.jasync.sql.db.mysql

import com.github.jasync.sql.db.interceptor.MdcQueryInterceptorSupplier
import com.github.jasync.sql.db.interceptor.QueryInterceptor
import com.github.jasync.sql.db.invoke
import com.github.jasync.sql.db.util.map
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.function.Supplier
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.slf4j.MDC

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
            val date = result["created_at_date"] as LocalDate

            assertEquals(2038, date.year)
            assertEquals(1, date.monthValue)
            assertEquals(19, date.dayOfMonth)

            val dateTime = result["created_at_datetime"] as LocalDateTime

            assertEquals(2013, dateTime.year)
            assertEquals(1, dateTime.monthValue)
            assertEquals(19, dateTime.dayOfMonth)
            assertEquals(3, dateTime.hour)
            assertEquals(14, dateTime.minute)
            assertEquals(7, dateTime.second)

            val timestamp = result["created_at_timestamp"] as LocalDateTime

            assertEquals(2020, timestamp.year)
            assertEquals(1, timestamp.monthValue)
            assertEquals(19, timestamp.dayOfMonth)
            assertEquals(3, timestamp.hour)
            assertEquals(14, timestamp.minute)
            assertEquals(7, timestamp.second)

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

        val date = LocalDate.of(2011, 9, 8)
        val dateTime = LocalDateTime.of(2012, 5, 27, 15, 29, 55, 1000)
        val timestamp = Timestamp.valueOf(dateTime)
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
            assertEquals(timestamp.toLocalDateTime(), row["created_at_timestamp"])
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

        val timestamp = LocalDateTime.of(2013, 1, 19, 3, 14, 7, 19 * 1000_000)
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
            val moment = LocalDateTime.now().withNano(0) // cut off millis to match timestamp
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

    @Test
    fun `be able to release prepared statements immediately`() {
        withConnection { connection ->
            val query = "select 1 as id , 'joe' as name"
            val result = executePreparedStatement(connection, query, emptyList(), true).rows

            assertThat(result[0]("name")).isEqualTo("joe")
            assertThat(result(0)("id")).isEqualTo(1L)
            assertThat(result.size).isEqualTo(1)

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

    @Test
    fun `test interceptor`() {
        try {
            System.setProperty("jasyncDoNotInterceptChecks", "true")
            val interceptor = ForTestingQueryInterceptor()
            MDC.put("a", "b")
            val mdcInterceptor = MdcQueryInterceptorSupplier()
            withConfigurableConnection(ContainerHelper.defaultConfiguration.copy(interceptors = listOf(Supplier<QueryInterceptor> { interceptor }, mdcInterceptor))) { connection ->
                executeQuery(connection, this.createTable)

                awaitFuture(connection.sendPreparedStatement(this.insert).map {
                    assertThat(MDC.get("a")).isEqualTo("b")
                })

                val result = assertNotNull(executePreparedStatement(connection, this.select).rows)
                assertEquals(1, result.size)
                assertEquals("Boogie Man", result[0]["name"])
            }
            assertThat(interceptor.preparedStatements.get()).isEqualTo(2)
            assertThat(interceptor.completedPreparedStatements.get()).isEqualTo(2)
            assertThat(MDC.get("a")).isEqualTo("b")
        } finally {
            System.getProperties().remove("jasyncDoNotInterceptChecks")
        }
    }
}
