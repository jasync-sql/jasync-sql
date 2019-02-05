package com.github.jasync.sql.db.mysql

import com.github.jasync.sql.db.QueryListener
import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.ResultSet
import com.github.jasync.sql.db.exceptions.InsufficientParametersException
import com.github.jasync.sql.db.invoke
import com.github.jasync.sql.db.mysql.exceptions.MySQLException
import io.netty.util.CharsetUtil
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime
import org.junit.Test
import java.math.BigDecimal
import java.time.Duration
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Supplier
import kotlin.test.assertEquals

class QuerySpec : ConnectionHelper() {


    @Test
    fun `"connection" should "be able to run a DML query"`() {
        withConnection { connection ->
            assertThat(executeQuery(connection, this.createTable).rowsAffected).isEqualTo(0)
        }
    }


    @Test
    fun `"connection" should   "raise an exception upon a bad statement" `() {
        withConnection { connection ->
            val e = verifyException(ExecutionException::class.java, MySQLException::class.java) {
                executeQuery(connection, "this is not SQL")
            }
            assertThat((e as MySQLException).errorMessage.sqlState).isEqualTo("#42000")
        }
    }


    @Test
    fun `"connection" should   "be able to select from a table" `() {

        withConnection { connection ->
            assertThat(executeQuery(connection, this.createTable).rowsAffected).isEqualTo(0)
            assertThat(executeQuery(connection, this.insert).rowsAffected).isEqualTo(1)
            val result: ResultSet = executeQuery(connection, this.select).rows

            assertThat(result[0]("id")).isEqualTo(1)
            assertThat(result(0)("name")).isEqualTo("Boogie Man")
        }

    }

    @Test
    fun `"connection" should   "be able to select from a table" - validate columnNames()`() {

        withConnection { connection ->
            assertThat(executeQuery(connection, this.createTable).rowsAffected).isEqualTo(0)
            assertThat(executeQuery(connection, this.insert).rowsAffected).isEqualTo(1)
            val result: ResultSet = executeQuery(connection, "select LAST_INSERT_ID()").rows
            executeQuery(connection, "select 0").rows
            assertThat(result.columnNames()).isEqualTo(listOf("LAST_INSERT_ID()"))
        }

    }

    @Test
    fun `"connection" should   "be able to select from a table with timestamps" `() {

        withConnection { connection ->
            executeQuery(connection, createTableTimeColumns)
            executeQuery(connection, insertTableTimeColumns)
            val result = executeQuery(connection, "SELECT * FROM posts").rows.get(0)

            val date = result("created_at_date") as LocalDate

            assertThat(date.getYear()).isEqualTo(2038)
            assertThat(date.getMonthOfYear()).isEqualTo(1)
            assertThat(date.getDayOfMonth()).isEqualTo(19)

            val dateTime = result("created_at_datetime") as LocalDateTime
            assertThat(dateTime.getYear()).isEqualTo(2013)
            assertThat(dateTime.getMonthOfYear()).isEqualTo(1)
            assertThat(dateTime.getDayOfMonth()).isEqualTo(19)
            assertThat(dateTime.getHourOfDay()).isEqualTo(3)
            assertThat(dateTime.getMinuteOfHour()).isEqualTo(14)
            assertThat(dateTime.getSecondOfMinute()).isEqualTo(7)

            val timestamp = result("created_at_timestamp") as LocalDateTime
            assertThat(timestamp.getYear()).isEqualTo(2020)
            assertThat(timestamp.getMonthOfYear()).isEqualTo(1)
            assertThat(timestamp.getDayOfMonth()).isEqualTo(19)
            assertThat(timestamp.getHourOfDay()).isEqualTo(3)
            assertThat(timestamp.getMinuteOfHour()).isEqualTo(14)
            assertThat(timestamp.getSecondOfMinute()).isEqualTo(7)


            assertThat(result("created_at_time")).isEqualTo(
                Duration.ofHours(3).plus(
                    Duration.ofMinutes(14).plus(
                        Duration.ofSeconds(7)
                    )
                )
            )

            val year = result("created_at_year") as Short

            assertThat(year).isEqualTo(1999)
        }

    }

    @Test
    fun `"connection" should   "be able to select from a table with the various numeric types" `() {

        withConnection { connection ->
            executeQuery(connection, createTableNumericColumns)
            executeQuery(connection, insertTableNumericColumns)
            val result = executeQuery(connection, "SELECT * FROM numbers").rows.get(0)

            assertThat(result("number_tinyint") as Byte).isEqualTo(-100)
            assertThat(result("number_smallint") as Short).isEqualTo(32766)
            assertThat(result("number_mediumint") as Int).isEqualTo(8388607)
            assertThat(result("number_int") as Int).isEqualTo(2147483647)
            assertThat(result("number_bigint") as Long).isEqualTo(9223372036854775807L)
            assertThat(result("number_decimal")).isEqualTo(BigDecimal("450.764491"))
            assertThat(result("number_float")).isEqualTo(14.7F)
            assertThat(result("number_double")).isEqualTo(87650.9876)
        }

    }

    @Test
    fun `"connection" should   "be able to read from a BLOB column when in text protocol" `() {
        val create = """CREATE TEMPORARY TABLE posts (
                   |       id INT NOT NULL AUTO_INCREMENT,
                   |       some_bytes BLOB not null,
                   |       primary key (id) )""".trimMargin("|")

        val insert = "insert into posts (some_bytes) values (?)"
        val select = "select * from posts"
        val bytes = "this is some text here".toByteArray(CharsetUtil.UTF_8)

        withConnection { connection ->
            executeQuery(connection, create)
            executePreparedStatement(connection, insert, listOf(bytes))
            val row = executeQuery(connection, select).rows.get(0)
            assertThat(row("id")).isEqualTo(1)
            assertThat(row("some_bytes")).isEqualTo(bytes)
        }
    }

    @Test
    fun `"connection" should   "have column names on result set" `() {

        val create = """CREATE TEMPORARY TABLE posts (
                   |       id INT NOT NULL AUTO_INCREMENT,
                   |       some_bytes BLOB not null,
                   |       primary key (id) )""".trimMargin("|")

        val createIdeas = """CREATE TEMPORARY TABLE ideas (
                        |       id INT NOT NULL AUTO_INCREMENT,
                        |       some_idea VARCHAR(255) NOT NULL,
                        |       primary key (id) )""".trimMargin("|")

        val select = "SELECT * FROM posts"
        val selectIdeas = "SELECT * FROM ideas"

        val matcher: (QueryResult) -> Unit = { result ->
            val columns = result.rows.columnNames()
            assertThat(columns).isEqualTo(listOf("id", "some_bytes"))
        }

        val ideasMatcher: (QueryResult) -> Unit = { result ->
            val columns = result.rows.columnNames()
            assertThat(columns).isEqualTo(listOf("id", "some_idea"))
        }

        withConnection { connection ->
            executeQuery(connection, create)
            executeQuery(connection, createIdeas)

            matcher(executePreparedStatement(connection, select))
            ideasMatcher(executePreparedStatement(connection, selectIdeas))

            matcher(executePreparedStatement(connection, select))
            ideasMatcher(executePreparedStatement(connection, selectIdeas))

            matcher(executeQuery(connection, select))
            ideasMatcher(executeQuery(connection, selectIdeas))
        }

    }

    @Test
    fun `"connection" should   "support BIT type" `() {

        val create =
            """CREATE TEMPORARY TABLE POSTS (
        | id INT NOT NULL AUTO_INCREMENT,
        | bit_column BIT(20),
        | primary key (id))
      """.trimMargin("|")

        val insert = "INSERT INTO POSTS (bit_column) VALUES (b'10000000')"
        val select = "SELECT * FROM POSTS"

        withConnection { connection ->
            executeQuery(connection, create)
            executeQuery(connection, insert)

            val rows = executeQuery(connection, select).rows
            assertThat(rows(0)("bit_column")).isEqualTo(byteArrayOf(0, 0, -128))

            val preparedRows = executePreparedStatement(connection, select).rows
            assertThat(preparedRows(0)("bit_column")).isEqualTo(byteArrayOf(0, 0, -128))
        }

    }

    @Test
    fun `"connection" should   "fail if number of args required is different than the number of provided parameters" `() {

        withConnection { connection ->
            verifyException(InsufficientParametersException::class.java) {
                executePreparedStatement(
                    connection,
                    "select * from some_table where c = ? and b = ?",
                    listOf("one", "two", "three")
                )
            }
        }

    }

    @Test
    fun `"connection" should   "select from another empty table with many columns" `() {
        withConnection { connection ->
            val create = """create temporary table test_11 (
                       |    id int primary key not null,
                       |    c2 text not null, c3 text not null, c4 text not null,
                       |    c5 text not null, c6 text not null, c7 text not null,
                       |    c8 text not null, c9 text not null, c10 text not null,
                       |    c11 text not null
                       |) ENGINE=InnoDB DEFAULT CHARSET=utf8;""".trimMargin("|")

            executeQuery(connection, create)

            val result = executeQuery(connection, "select * from test_11")

            assertThat(result.rows.size).isEqualTo(0)
        }
    }

    @Test
    fun `"connection" should   "select from an empty table with many columns" `() {

        withConnection { connection ->

            val create = """create temporary table test_10 (
                       |    id int primary key not null,
                       |    c2 text not null, c3 text not null, c4 text not null,
                       |    c5 text not null, c6 text not null, c7 text not null,
                       |    c8 text not null, c9 text not null, c10 text not null
                       |) ENGINE=InnoDB DEFAULT CHARSET=utf8;""".trimMargin("|")

            executeQuery(connection, create)

            val result = executeQuery(connection, "select * from test_10")

            assertThat(result.rows.size).isEqualTo(0)
        }

    }

    @Test
    fun `"connection" should   "select from a json column" `() {

        val create = "create temporary table jsons (id char(4), data json)"

        val insert = """  insert jsons values
                   |  ('json', '{"a": 1}')""".trimMargin("|")


        withConnection { connection ->
            executeQuery(connection, create)
            executeQuery(connection, insert)
            val result = executeQuery(connection, "select data from jsons").rows

            assertThat(result.size).isEqualTo(1)

            assertThat((result(0)("data"))).isEqualTo("""{"a": 1}""")
        }

    }

    @Test
    fun `"connection listener" should   "be able to select from a table" `() {
        val queries = AtomicInteger()
        val completed = AtomicInteger()
        val errors = AtomicInteger()
        val listener = object : QueryListener {
            override fun onPreparedStatement(query: String, values: List<Any?>) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onPreparedStatementComplete(result: QueryResult) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onPreparedStatementError(query: Throwable) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onQuery(query: String) {
                queries.getAndIncrement()
            }

            override fun onQueryComplete(result: QueryResult) {
                completed.getAndIncrement()
            }

            override fun onQueryError(query: Throwable) {
                errors.getAndIncrement()
            }
        }
        withConfigurablePool(poolConfiguration().copy(listeners = listOf(Supplier<QueryListener> { listener })))
        { connection ->
            assertThat(executeQuery(connection, this.createTable).rowsAffected).isEqualTo(0)
            assertThat(executeQuery(connection, this.insert).rowsAffected).isEqualTo(1)
            val result: ResultSet = executeQuery(connection, this.select).rows

            assertThat(result[0]("id")).isEqualTo(1)
            assertThat(result(0)("name")).isEqualTo("Boogie Man")
        }
        assertEquals(3, queries.get())
        assertEquals(3, completed.get())
        assertEquals(0, errors.get())
    }

    @Test
    fun `"connection listener with error" should   "throws error" `() {
        val queries = AtomicInteger()
        val completed = AtomicInteger()
        val errors = AtomicInteger()
        val listener = object : QueryListener {
            override fun onPreparedStatement(query: String, values: List<Any?>) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onPreparedStatementComplete(result: QueryResult) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onPreparedStatementError(query: Throwable) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onQuery(query: String) {
                queries.getAndIncrement()
            }

            override fun onQueryComplete(result: QueryResult) {
                completed.getAndIncrement()
            }

            override fun onQueryError(query: Throwable) {
                errors.getAndIncrement()
            }
        }
        withConfigurablePool(poolConfiguration().copy(listeners = listOf(Supplier<QueryListener> { listener })))
        { connection ->
            assertThat(executeQuery(connection, this.createTable).rowsAffected).isEqualTo(0)
            assertThat(executeQuery(connection, this.insert).rowsAffected).isEqualTo(1)
            val result: ResultSet = executeQuery(connection, this.select).rows

            assertThat(result[0]("id")).isEqualTo(1)
            assertThat(result(0)("name")).isEqualTo("Boogie Man")
        }
        assertEquals(3, queries.get())
        assertEquals(3, completed.get())
        assertEquals(0, errors.get())
    }

    private fun poolConfiguration() =
        ContainerHelper.defaultConfiguration.copy(queryTimeout = (Duration.ofSeconds(1)))
}


