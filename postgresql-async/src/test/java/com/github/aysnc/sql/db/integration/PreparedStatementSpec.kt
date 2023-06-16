package com.github.aysnc.sql.db.integration

import com.github.aysnc.sql.db.verifyException
import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.exceptions.InsufficientParametersException
import com.github.jasync.sql.db.interceptor.LoggingInterceptorSupplier
import com.github.jasync.sql.db.interceptor.MdcQueryInterceptorSupplier
import com.github.jasync.sql.db.interceptor.QueryInterceptor
import com.github.jasync.sql.db.invoke
import com.github.jasync.sql.db.postgresql.exceptions.GenericDatabaseException
import com.github.jasync.sql.db.util.length
import com.github.jasync.sql.db.util.map
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.slf4j.MDC
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

class PreparedStatementSpec : DatabaseTestHelper() {

    private val filler = " ".repeat(64)

    val messagesCreate = """CREATE TEMP TABLE messages
                         (
                           id bigserial NOT NULL,
                           content character varying(255) NOT NULL,
                           moment date NULL,
                           CONSTRAINT bigserial_column_pkey PRIMARY KEY (id )
                         )"""
    val messagesInsert = "INSERT INTO messages $filler (content,moment) VALUES (?,?) RETURNING id"
    val messagesInsertReverted = "INSERT INTO messages $filler (moment,content) VALUES (?,?) RETURNING id"
    val messagesUpdate = "UPDATE messages SET content = ?, moment = ? WHERE id = ?"
    val messagesSelectOne = "SELECT id, content, moment FROM messages WHERE id = ?"
    val messagesSelectByMoment = "SELECT id, content, moment FROM messages WHERE moment = ?"
    val messagesSelectAll = "SELECT id, content, moment FROM messages"
    val messagesSelectEscaped = "SELECT id, content, moment FROM messages WHERE content LIKE '%??%' AND id > ?"

    @Test
    fun `prepared statements should support prepared statement with more than 64 characters`() {
        withHandler { handler ->

            val firstContent = "Some Moment"
            val secondContent = "Some Other Moment"
            val date = LocalDate.now()

            executeDdl(handler, this.messagesCreate)
            executePreparedStatement(handler, this.messagesInsert, listOf(firstContent, date))
            executePreparedStatement(handler, this.messagesInsertReverted, listOf(date, secondContent))

            val rows = executePreparedStatement(handler, this.messagesSelectAll).rows

            assertThat(rows.length).isEqualTo(2)

            assertThat(rows(0)("id")).isEqualTo(1L)
            assertThat(rows(0)("content")).isEqualTo(firstContent)
            assertThat(rows(0)("moment")).isEqualTo(date)

            assertThat(rows(1)("id")).isEqualTo(2L)
            assertThat(rows(1)("content")).isEqualTo(secondContent)
            assertThat(rows(1)("moment")).isEqualTo(date)
        }
    }

    @Test
    fun `prepared statements should execute a prepared statement without any parameters multiple times`() {
        withHandler { handler ->
            executeDdl(handler, this.messagesCreate)
            executePreparedStatement(handler, "UPDATE messages SET content = content")
            executePreparedStatement(handler, "UPDATE messages SET content = content")
        }
    }

    @Test
    fun `prepared statements should raise an exception if the parameter count is different from the given parameters count`() {
        withHandler { handler ->
            executeDdl(handler, this.messagesCreate)
            verifyException(InsufficientParametersException::class.java) {
                executePreparedStatement(handler, this.messagesSelectOne)
            }
        }
    }

    @Test
    fun `prepared statements should run two different prepared statements in sequence and get the right values`() {
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

        withHandler { handler ->
            executeDdl(handler, this.messagesCreate)
            executeDdl(handler, create)

            (1.until(4)).forEach { x ->
                executePreparedStatement(handler, this.messagesInsert, listOf(message, moment))
                executePreparedStatement(handler, insert, listOf(otherMoment, otherMessage))

                val result = executePreparedStatement(handler, this.messagesSelectAll).rows
                assertThat(result.size).isEqualTo(x)
                assertThat(result.columnNames()).isEqualTo(listOf("id", "content", "moment"))
                assertThat(result(x - 1)("moment")).isEqualTo(moment)
                assertThat(result(x - 1)("content")).isEqualTo(message)

                val otherResult = executePreparedStatement(handler, select).rows
                assertThat(otherResult.size).isEqualTo(x)
                assertThat(otherResult.columnNames()).isEqualTo(listOf("id", "other_moment", "other_content"))
                assertThat(otherResult(x - 1)("other_moment")).isEqualTo(otherMoment)
                assertThat(otherResult(x - 1)("other_content")).isEqualTo(otherMessage)
            }
        }
    }

    @Test
    fun `prepared statements should support prepared statement with Option parameters (Some or None)`() {
        withHandler { handler ->

            val firstContent = "Some Moment"
            val secondContent = "Some Other Moment"
            val date = LocalDate.now()

            executeDdl(handler, this.messagesCreate)
            executePreparedStatement(handler, this.messagesInsert, listOf(firstContent, null))
            executePreparedStatement(handler, this.messagesInsert, listOf((secondContent), (date)))

            val rows = executePreparedStatement(handler, this.messagesSelectAll).rows

            assertThat(rows.length).isEqualTo(2)

            assertThat(rows(0)("id")).isEqualTo(1L)
            assertThat(rows(0)("content")).isEqualTo(firstContent)
            assertThat(rows(0)("moment")).isEqualTo(null)

            assertThat(rows(1)("id")).isEqualTo(2L)
            assertThat(rows(1)("content")).isEqualTo(secondContent)
            assertThat(rows(1)("moment")).isEqualTo(date)
        }
    }

    @Test
    fun `prepared statements should supports sending null first and then an actual value for the fields`() {
        withHandler { handler ->

            val firstContent = "Some Moment"
            val secondContent = "Some Other Moment"
            val date = LocalDate.now()

            executeDdl(handler, this.messagesCreate)
            executePreparedStatement(handler, this.messagesInsert, listOf(firstContent, null))
            executePreparedStatement(handler, this.messagesInsert, listOf(secondContent, date))

            val rows = executePreparedStatement(handler, this.messagesSelectByMoment, listOf(null)).rows
            assertThat(rows.size).isEqualTo(0)

            /*
            PostgreSQL does not know how to handle NULL parameters for a query in a prepared statement,
            you have to use IS NULL if you want to make use of it.

      assertThat(          rows.length).isEqualTo(1)

      assertThat(          rows(0)("id")).isEqualTo(1)
      assertThat(          rows(0)("content")).isEqualTo(firstContent)
      assertThat(          rows(0)("moment")).isEqualTo(null)
            */

            val rowsWithoutNull = executePreparedStatement(handler, this.messagesSelectByMoment, listOf(date)).rows
            assertThat(rowsWithoutNull.size).isEqualTo(1)
            assertThat(rowsWithoutNull(0)("id")).isEqualTo(2L)
            assertThat(rowsWithoutNull(0)("content")).isEqualTo(secondContent)
            assertThat(rowsWithoutNull(0)("moment")).isEqualTo(date)
        }
    }

    @Test
    fun `prepared statements should support prepared statement with escaped placeholders`() {
        withHandler { handler ->

            val firstContent = "Some? Moment"
            val secondContent = "Some Other Moment"
            val date = LocalDate.now()

            executeDdl(handler, this.messagesCreate)
            executePreparedStatement(handler, this.messagesInsert, listOf((firstContent), null))
            executePreparedStatement(handler, this.messagesInsert, listOf((secondContent), (date)))

            val rows = executePreparedStatement(handler, this.messagesSelectEscaped, listOf(0)).rows

            assertThat(rows.length).isEqualTo(1)

            assertThat(rows(0)("id")).isEqualTo(1L)
            assertThat(rows(0)("content")).isEqualTo(firstContent)
            assertThat(rows(0)("moment")).isEqualTo(null)
        }
    }

    @Test
    fun `prepared statements should support handling of enum types`() {
        withHandler { handler ->
            val create = """CREATE TEMP TABLE messages
                         |(
                         |id bigserial NOT NULL,
                         |feeling example_mood,
                         |CONSTRAINT bigserial_column_pkey PRIMARY KEY (id )
                         |);""".trimMargin("|")
            val insert = "INSERT INTO messages (feeling) VALUES (?) RETURNING id"
            val select = "SELECT * FROM messages"

            executeDdl(handler, create)

            executePreparedStatement(handler, insert, listOf("sad"))

            val result = executePreparedStatement(handler, select).rows

            assertThat(result.size).isEqualTo(1)
            assertThat(result(0)("id")).isEqualTo(1L)
            assertThat(result(0)("feeling")).isEqualTo("sad")
        }
    }

    @Test
    fun `prepared statements should support handling JSON type`() {
        withHandler { handler ->
            val create = """create temp table people
                           |(
                           |id bigserial primary key,
                           |addresses json,
                           |phones json
                           |);""".trimMargin("|")

            val insert = "INSERT INTO people (addresses, phones) VALUES (?,?) RETURNING id"
            val select = "SELECT * FROM people"
            val addresses = """[ {"Home" : {"city" : "Tahoe", "state" : "CA"}} ]"""
            val phones = """[ "925-575-0415", "916-321-2233" ]"""

            executeDdl(handler, create)
            executePreparedStatement(handler, insert, listOf(addresses, phones))
            val result = executePreparedStatement(handler, select).rows

            assertThat(result(0)("addresses")).isEqualTo(addresses)
            assertThat(result(0)("phones")).isEqualTo(phones)
        }
    }

    @Test
    fun `prepared statements should support select bind value`() {
        withHandler { handler ->
            val string = "someString"
            val result = executePreparedStatement(handler, "SELECT CAST(? AS VARCHAR)", listOf(string)).rows
            assertThat(result(0)(0)).isEqualTo(string)
        }
    }

    @Test
    fun `prepared statements should fail if prepared statement has more variables than it was given`() {
        withHandler { handler ->
            executeDdl(handler, messagesCreate)
            verifyException(InsufficientParametersException::class.java) {
                handler.sendPreparedStatement(
                    "SELECT * FROM messages WHERE content = ? AND moment = ?",
                    listOf("some content")
                )
            }
        }
    }

    @Test
    fun `prepared statements should run prepared statement twice with bad and good values`() {
        withHandler { handler ->
            val content = "Some Moment"

            val query = "SELECT content FROM messages WHERE id = ?"

            executeDdl(handler, messagesCreate)
            executePreparedStatement(handler, this.messagesInsert, listOf((content), null))
            verifyException(ExecutionException::class.java, GenericDatabaseException::class.java) {
                executePreparedStatement(handler, query, listOf("undefined"))
            }
            val result = executePreparedStatement(handler, query, listOf(1)).rows
            assertThat(result(0)(0)).isEqualTo(content)
        }
    }

    @Test
    fun `prepared statements should support UUID`() {
        withHandler { handler ->
            val create = """create temp table uuids
                           |(
                           |id bigserial primary key,
                           |my_id uuid
                           |);""".trimMargin("|")

            val insert = "INSERT INTO uuids (my_id) VALUES (?) RETURNING id"
            val select = "SELECT * FROM uuids"

            val uuid = UUID.randomUUID()

            executeDdl(handler, create)
            executePreparedStatement(handler, insert, listOf(uuid))
            val result = executePreparedStatement(handler, select).rows

            assertThat(result(0)("my_id") as UUID).isEqualTo(uuid)
        }
    }

    @Test
    fun `prepared statements should support UUID array`() {
        withHandler { handler ->
            val create = """create temp table uuids
                           |(
                           |id bigserial primary key,
                           |my_id uuid[]
                           |);""".trimMargin("|")

            val insert = "INSERT INTO uuids (my_id) VALUES (?) RETURNING id"
            val select = "SELECT * FROM uuids"

            val uuid1 = UUID.randomUUID()
            val uuid2 = UUID.randomUUID()

            executeDdl(handler, create)
            executePreparedStatement(handler, insert, listOf(listOf(uuid1, uuid2)))
            val result = executePreparedStatement(handler, select).rows

            @Suppress("UNCHECKED_CAST")
            assertThat(result(0)("my_id") as List<UUID>).isEqualTo(listOf(uuid1, uuid2))
        }
    }

    @Test
    fun `prepared statements should deallocates prepared statements`() {
        withHandler { handler ->
            val firstContent = "Some Moment"
            val secondContent = "Some other moment"
            val date = LocalDate.now()

            executeDdl(handler, this.messagesCreate)
            executePreparedStatement(handler, this.messagesInsert, listOf(firstContent, null))

            val statement = "INSERT INTO messages                                                                  " +
                "(content,moment) VALUES ($1,$2) RETURNING id"
            val listedStatements = executeQuery(handler, "SELECT * FROM pg_prepared_statements").rows
            assertThat(listedStatements.size).isEqualTo(1)

            assertThat(listedStatements(0)(1)).isEqualTo(statement)

            releasePreparedStatement(handler, this.messagesInsert)

            assertThat(executeQuery(handler, "SELECT * FROM pg_prepared_statements").rows.size).isEqualTo(0)

            executePreparedStatement(handler, this.messagesInsert, listOf(secondContent, date))

            val rows = executePreparedStatement(handler, this.messagesSelectAll).rows

            assertThat(rows.length).isEqualTo(2)

            assertThat(rows(0)("id")).isEqualTo(1L)
            assertThat(rows(0)("content")).isEqualTo(firstContent)
            assertThat(rows(0)("moment")).isNull()

            assertThat(rows(1)("id")).isEqualTo(2L)
            assertThat(rows(1)("content")).isEqualTo(secondContent)
            assertThat(rows(1)("moment")).isEqualTo(date)
        }
    }

    @Test
    fun `prepared statements should deallocates prepared statements when release immediately`() {
        withHandler { handler ->
            val firstContent = "Some Moment"
            val secondContent = "Some other moment"
            val date = LocalDate.now()

            executeDdl(handler, this.messagesCreate)
            executePreparedStatement(handler, this.messagesInsert, listOf(firstContent, null), true)

            assertThat(executeQuery(handler, "SELECT * FROM pg_prepared_statements").rows.size).isEqualTo(0)

            executePreparedStatement(handler, this.messagesInsert, listOf(secondContent, date))

            val rows = executePreparedStatement(handler, this.messagesSelectAll).rows

            assertThat(rows.length).isEqualTo(2)

            assertThat(rows(0)("id")).isEqualTo(1L)
            assertThat(rows(0)("content")).isEqualTo(firstContent)
            assertThat(rows(0)("moment")).isNull()

            assertThat(rows(1)("id")).isEqualTo(2L)
            assertThat(rows(1)("content")).isEqualTo(secondContent)
            assertThat(rows(1)("moment")).isEqualTo(date)
        }
    }

    @Test
    fun `handler should handle interceptors`() {
        val interceptor = ForTestingQueryInterceptor()
        MDC.put("a", "b")
        val mdcInterceptor = MdcQueryInterceptorSupplier()
        val configuration = Configuration(
            ContainerHelper.defaultConfiguration.username,
            ContainerHelper.defaultConfiguration.host,
            ContainerHelper.defaultConfiguration.port,
            ContainerHelper.defaultConfiguration.password,
            ContainerHelper.defaultConfiguration.database,
            interceptors = listOf(Supplier<QueryInterceptor> { interceptor }, mdcInterceptor, LoggingInterceptorSupplier())
        )
        withHandler(configuration) { handler ->
            val firstContent = "Some Moment"

            executeDdl(handler, this.messagesCreate)
            handler.sendPreparedStatement(this.messagesInsert, listOf(firstContent, null), true)
                .map { assertThat(MDC.get("a")).isEqualTo("b") }
                .get(5, TimeUnit.SECONDS)
        }
        assertThat(interceptor.preparedStatements.get()).isEqualTo(1)
        assertThat(interceptor.completedPreparedStatements.get()).isEqualTo(1)
    }
}
