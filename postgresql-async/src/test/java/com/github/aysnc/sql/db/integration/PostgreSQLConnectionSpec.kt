package com.github.aysnc.sql.db.integration

import com.github.aysnc.sql.db.integration.ContainerHelper.defaultConfiguration
import com.github.aysnc.sql.db.verifyException
import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.column.DateEncoderDecoder
import com.github.jasync.sql.db.column.TimeEncoderDecoder
import com.github.jasync.sql.db.column.TimestampEncoderDecoder
import com.github.jasync.sql.db.invoke
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import com.github.jasync.sql.db.postgresql.exceptions.QueryMustNotBeNullOrEmptyException
import com.github.jasync.sql.db.util.ExecutorServiceUtils
import com.github.jasync.sql.db.util.flatMapAsync
import com.github.jasync.sql.db.util.head
import com.github.jasync.sql.db.util.mapAsync
import io.netty.buffer.Unpooled
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.LocalDateTime
import org.junit.Test
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit


class PostgreSQLConnectionSpec : DatabaseTestHelper() {


    private val sampleArray: ByteArray = byteArrayOf(83, 97, 121, 32, 72, 101, 108, 108, 111, 32, 116, 111, 32, 77, 121, 32, 76, 105, 116, 116, 108, 101, 32, 70, 114, 105, 101, 110, 100)

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


    @Test
    fun `"handler" should     "connect to the database"`() {

        withHandler { handler ->
            assertThat(handler.isReadyForQuery()).isTrue()
        }

    }

    @Test
    fun `"handler" should     "create a table in the database"`() {

        withHandler { handler ->
            assertThat(executeDdl(handler, this.create)).isEqualTo(0)
        }

    }

    @Test
    fun `"handler" should     "insert a row in the database"`() {

        withHandler { handler ->
            executeDdl(handler, this.create)
            assertThat(executeDdl(handler, this.insert, 1)).isEqualTo(1)

        }

    }

    @Test
    fun `"handler" should     "select rows in the database"`() {

        withHandler { handler ->
            executeDdl(handler, this.create)
            executeDdl(handler, this.insert, 1)
            val result = executeQuery(handler, this.select)

            val row = result.rows!!.get(0)

            assertThat(row(0)).isEqualTo(1L)
            assertThat(row(1)).isEqualTo(10.toShort())
            assertThat(row(2)).isEqualTo(11)
            assertThat(row(3).toString()).isEqualTo("14.9990")
            assertThat(row(4).toString()).isEqualTo(78.34.toString())
            assertThat(row(5)).isEqualTo(15.68)
            assertThat(row(6)).isEqualTo(1)
            assertThat(row(7)).isEqualTo("this is a varchar field")
            assertThat(row(8)).isEqualTo("this is a long text field")
            assertThat(row(9)).isEqualTo(TimestampEncoderDecoder.Instance.decode("1984-08-06 22:13:45.888888"))
            assertThat(row(10)).isEqualTo(DateEncoderDecoder.decode("1984-08-06"))
            assertThat(row(11)).isEqualTo(TimeEncoderDecoder.Instance.decode("22:13:45.888888"))
            assertThat(row(12)).isEqualTo(true)
            assertThat(row(13)).isInstanceOf(java.lang.Long::class.java)
            assertThat(row(13) as Long).isGreaterThan(0L)


        }

    }

    @Test
    fun `"handler" should     "select rows that has duplicate column names"`() {

        withHandler { handler ->
            val result = executeQuery(handler, "SELECT 1 COL, 2 COL")

            val row = result.rows!!.get(0)

            assertThat(row(0)).isEqualTo(1)
            assertThat(row(1)).isEqualTo(2)

        }

    }

    @Test
    fun `"handler" should     "execute a prepared statement"`() {

        withHandler { handler ->
            executeDdl(handler, this.preparedStatementCreate)
            executeDdl(handler, this.preparedStatementInsert, 1)
            val result = executePreparedStatement(handler, this.preparedStatementSelect)

            val row = result!!.rows!!.get(0)


            assertThat(row(0)).isEqualTo(1L)
            assertThat(row(1)).isEqualTo("John Doe")


        }

    }

    @Test
    fun `"handler" should     "execute a prepared statement , parameters"`() {

        withHandler { handler ->
            executeDdl(handler, this.preparedStatementCreate)
            executeDdl(handler, this.preparedStatementInsert, 1)
            executeDdl(handler, this.preparedStatementInsert2, 1)
            executeDdl(handler, this.preparedStatementInsert3, 1)

            val select = "select * from prepared_statement_test where name like ?"

            val queryResult = executePreparedStatement(handler, select, listOf("Peter Parker"))
            val row = queryResult!!.rows!!.get(0)

            val queryResult2 = executePreparedStatement(handler, select, listOf("Mary Jane"))
            val row2 = queryResult2!!.rows!!.get(0)

            assertThat(row(0)).isEqualTo(3L)
            assertThat(row(1)).isEqualTo("Peter Parker")

            assertThat(row2(0)).isEqualTo(2L)
            assertThat(row2(1)).isEqualTo("Mary Jane")

        }

    }


    @Test
    fun `"handler" should     "transaction and flatmap example"`() {

        val handler: Connection = PostgreSQLConnection(defaultConfiguration)
        val result: CompletableFuture<QueryResult> = handler.connect()
                .mapAsync(ExecutorServiceUtils.CommonPool) { parameters -> handler }
                .flatMapAsync(ExecutorServiceUtils.CommonPool) { connection -> connection.sendQuery("BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ") }
                .flatMapAsync(ExecutorServiceUtils.CommonPool) { query -> handler.sendQuery("SELECT 0") }
                .flatMapAsync(ExecutorServiceUtils.CommonPool) { query -> handler.sendQuery("COMMIT").mapAsync(ExecutorServiceUtils.CommonPool) { value -> query } }

        val queryResult: QueryResult = result.get(5, TimeUnit.SECONDS)

        assertThat(queryResult.rows!!.get(0)(0)).isEqualTo(0)

    }

    @Test
    fun `"handler" should     "use RETURNING in an insert statement"`() {

        withHandler { connection ->
            executeDdl(connection, this.preparedStatementCreate)
            val result = executeQuery(connection, this.preparedStatementInsertReturning)
            assertThat(result.rows!!.get(0)("id")).isEqualTo(1L)
        }

    }

    @Test
    fun `"handler" should     "execute a prepared statement , limit"`() {

        withHandler { handler ->
            executeDdl(handler, this.preparedStatementCreate)
            executeDdl(handler, this.preparedStatementInsert, 1)
            executeDdl(handler, this.preparedStatementInsert2, 1)
            executeDdl(handler, this.preparedStatementInsert3, 1)

            val result = executePreparedStatement(handler, "select * from prepared_statement_test LIMIT 1")!!.rows!!.get(0)

            assertThat(result("name")).isEqualTo("John Doe")
        }

    }

    @Test
    fun `"handler" should     "execute an empty query"`() {

        withHandler { handler ->
            verifyException(QueryMustNotBeNullOrEmptyException::class.java) {

                assertThat(executeQuery(handler, "").rows).isNull()
            }
        }

    }

    @Test
    fun `"handler" should     "execute an whitespace query"`() {

        withHandler { handler ->
            verifyException(ExecutionException::class.java, QueryMustNotBeNullOrEmptyException::class.java) {
                assertThat(executeQuery(handler, "   ").rows).isNull()
            }
        }

    }

    @Test
    fun `"handler" should     "execute multiple prepared statements"`() {
        withHandler { handler ->
            executeDdl(handler, this.preparedStatementCreate)
            (0 until 1000).forEach {
                executePreparedStatement(handler, this.preparedStatementInsert)
            }
        }
    }

    @Test
    fun `"handler" should     "load data from a bytea column"`() {

        val create = """create temp table file_samples (
        id bigserial not null,
        content bytea not null,
        constraint bigserial_column_pkey primary key (id)
      )"""

        val insert = "insert into file_samples (content) values ( E'\\\\x5361792048656c6c6f20746f204d79204c6974746c6520467269656e64' ) "
        val select = "select * from file_samples"

        withHandler { handler ->
            executeDdl(handler, create)
            executeQuery(handler, insert)
            val rows = executeQuery(handler, select).rows!!

            assertThat(rows(0)("content") as ByteArray).isEqualTo(sampleArray)

        }

    }

    @Test
    fun `"handler" should     "send data to a bytea column"`() {
        val create = """create temp table file_samples (
        id bigserial not null,
        content bytea not null,
        constraint bigserial_column_pkey primary key (id)
      )"""

        val insert = "insert into file_samples (content) values ( ? ) "
        val select = "select * from file_samples"

        withHandler { handler ->

            executeDdl(handler, create)
            //log.debug("executed create")
            executePreparedStatement(handler, insert, listOf(sampleArray))
            executePreparedStatement(handler, insert, listOf(ByteBuffer.wrap(sampleArray)))
            executePreparedStatement(handler, insert, listOf(Unpooled.copiedBuffer(sampleArray)))
            //log.debug("executed prepared statement")
            val rows = executeQuery(handler, select).rows!!

            assertThat(rows(0)("content") as ByteArray).isEqualTo(sampleArray)
            assertThat(rows(1)("content") as ByteArray).isEqualTo(sampleArray)
            assertThat(rows(2)("content") as ByteArray).isEqualTo(sampleArray)
        }

    }

    @Test
    fun `"handler" should     "insert a LocalDateTime"`() {

        withHandler { handler ->
            executePreparedStatement(handler, "CREATE TEMP TABLE test(t TIMESTAMP)")
            val date1 = LocalDateTime()
            executePreparedStatement(handler, "INSERT INTO test(t) VALUES(?)", listOf(date1))
            val result = executePreparedStatement(handler, "SELECT t FROM test")
            val date2 = (result!!.rows!!.head)(0)
            assertThat(date1).isEqualTo(date2)
        }

    }

    @Test
    fun `"handler" should     "insert ,out return after select"`() {

        withHandler { handler ->
            executeDdl(handler, this.preparedStatementCreate)
            executeDdl(handler, this.preparedStatementInsert, 1)
            executeDdl(handler, this.preparedStatementSelect, 1)
            val result = executeQuery(handler, this.preparedStatementInsert2)

            assertThat(result.rows).isNull()
        }

    }


}
