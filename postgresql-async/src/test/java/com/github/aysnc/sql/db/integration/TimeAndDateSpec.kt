package com.github.aysnc.sql.db.integration

import com.github.jasync.sql.db.invoke
import com.github.jasync.sql.db.util.head
import com.github.jasync.sql.db.util.length
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.Ignore
import org.junit.Test
import org.threeten.extra.PeriodDuration
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.Period
import java.time.ZoneOffset

class TimeAndDateSpec : DatabaseTestHelper() {

    @Test
    fun `when processing times and dates should support a time object`() {

        withHandler { handler ->
            val create = """CREATE TEMP TABLE messages
                         (
                           id bigserial NOT NULL,
                           moment time NOT NULL,
                           CONSTRAINT bigserial_column_pkey PRIMARY KEY (id )
                         )"""

            executeDdl(handler, create)
            executePreparedStatement(handler, "INSERT INTO messages (moment) VALUES (?)", listOf(LocalTime.of(4, 5, 6)))

            val rows = executePreparedStatement(handler, "select * from messages").rows

            val time = rows[0]("moment") as LocalTime

            assertThat(time.hour).isEqualTo(4)
            assertThat(time.minute).isEqualTo(5)
            assertThat(time.second).isEqualTo(6)
        }
    }

    @Test
    fun `when processing times and dates should support a time object, microseconds`() {

        withHandler { handler ->
            val create = """CREATE TEMP TABLE messages
                         (
                           id bigserial NOT NULL,
                           moment time(6) NOT NULL,
                           CONSTRAINT bigserial_column_pkey PRIMARY KEY (id )
                         )"""

            executeDdl(handler, create)
            executePreparedStatement(
                handler,
                "INSERT INTO messages (moment) VALUES (?)",
                listOf(LocalTime.of(4, 5, 6, 134 * 1000_000))
            )

            val rows = executePreparedStatement(handler, "select * from messages").rows

            val time = rows(0)("moment") as LocalTime

            assertThat(time.hour).isEqualTo(4)
            assertThat(time.minute).isEqualTo(5)
            assertThat(time.second).isEqualTo(6)
            assertThat(time.nano).isEqualTo(134 * 1000_000)
        }
    }

    @Ignore("need to find a way to implement this")
    @Test
    fun `when processing times and dates should support a time with timezone object`() {

        withHandler { handler ->
            val create = """CREATE TEMP TABLE messages
                         (
                           id bigserial NOT NULL,
                           moment time with time zone NOT NULL,
                           CONSTRAINT bigserial_column_pkey PRIMARY KEY (id )
                         )"""

            executeDdl(handler, create)
            executeQuery(handler, "INSERT INTO messages (moment) VALUES ('04:05:06 -3:00')")

            val rows = executePreparedStatement(handler, "select * from messages").rows

            val time = rows(0)("moment") as LocalTime

            assertThat(time.hour).isEqualTo(4)
            assertThat(time.minute).isEqualTo(5)
            assertThat(time.second).isEqualTo(6)
        }
    }

    @Test
    fun `when processing times and dates should support timestamp with timezone`() {
        withHandler { handler ->

            val create = """CREATE TEMP TABLE messages
                         (
                           id bigserial NOT NULL,
                           moment timestamp with time zone NOT NULL,
                           CONSTRAINT bigserial_column_pkey PRIMARY KEY (id )
                         )"""

            executeDdl(handler, create)
            executeQuery(handler, "INSERT INTO messages (moment) VALUES ('1999-01-08 04:05:06 -3:00')")
            val rows = executePreparedStatement(handler, "SELECT * FROM messages").rows

            assertThat(rows.length).isEqualTo(1)

            val dateTime = rows(0)("moment") as OffsetDateTime

            // Note: Since this assertion depends on Brazil locale, I think epoch time assertion is preferred
            // assertThat(          // dateTime.getZone.toTimeZone.getRawOffset).isEqualTo(-10800000)
            assertThat(dateTime.toInstant().toEpochMilli()).isEqualTo(915779106000L)
        }
    }

    @Test
    fun `when processing times and dates should support timestamp with timezone and microseconds`() {

        (1 until 6).forEach { index ->
            withHandler { handler ->

                val create = """CREATE TEMP TABLE messages
                         (
                           id bigserial NOT NULL,
                           moment timestamp(%d) with time zone NOT NULL,
                           CONSTRAINT bigserial_column_pkey PRIMARY KEY (id )
                         )""".format(index)

                executeDdl(handler, create)

                val seconds = (index.toString().repeat(index)).toLong()

                executeQuery(
                    handler,
                    "INSERT INTO messages (moment) VALUES ('1999-01-08 04:05:06.%d -3:00')".format(seconds)
                )
                val rows = executePreparedStatement(handler, "SELECT * FROM messages").rows

                assertThat(rows.length).isEqualTo(1)

                val dateTime = rows(0)("moment") as OffsetDateTime

                // Note: Since this assertion depends on Brazil locale, I think epoch time assertion is preferred
                // dateTime.getZone.toTimeZone.getRawOffset).isEqualTo(-10800000)
                assertThat(dateTime.toInstant().toEpochMilli()).isGreaterThan(915779106000L)
                assertThat(dateTime.toInstant().toEpochMilli()).isLessThan(915779107000L)
            }
        }
    }

    @Test
    fun `when processing times and dates should support current_timestamp with timezone`() {
        withHandler { handler ->

            val millis = System.currentTimeMillis()

            val create = """CREATE TEMP TABLE messages
                         (
                           id bigserial NOT NULL,
                           moment timestamp with time zone NOT NULL,
                           CONSTRAINT bigserial_column_pkey PRIMARY KEY (id )
                         )"""

            executeDdl(handler, create)
            executeQuery(handler, "INSERT INTO messages (moment) VALUES (current_timestamp)")
            val rows = executePreparedStatement(handler, "SELECT * FROM messages").rows

            assertThat(rows.length).isEqualTo(1)

            val dateTime = rows(0)("moment") as OffsetDateTime

            assertThat(dateTime.toInstant().toEpochMilli()).isCloseTo(millis, Offset.offset(1000L))
        }
    }

    @Test
    fun `when processing times and dates should handle sending a time with timezone and return a LocalDateTime for a timestamp without timezone column`() {

        withHandler { conn ->
            val date = OffsetDateTime.ofInstant(Instant.ofEpochMilli(2190319), ZoneOffset.UTC)

            executePreparedStatement(conn, "CREATE TEMP TABLE TEST(T TIMESTAMP)")
            executePreparedStatement(conn, "INSERT INTO TEST(T) VALUES(?)", listOf(date))
            val result = executePreparedStatement(conn, "SELECT T FROM TEST")
            val date2 = (result.rows.head)(0)
            assertThat(date2).isEqualTo(date.toLocalDateTime())
        }
    }

    @Test
    fun `when processing times and dates should supports sending a local date and later a date time object for the same field`() {

        withHandler { conn ->
            val date = OffsetDateTime.of(2016, 3, 5, 0, 0, 0, 0, ZoneOffset.UTC)

            executePreparedStatement(conn, "CREATE TEMP TABLE TEST(T TIMESTAMP)")
            executePreparedStatement(conn, "INSERT INTO TEST(T) VALUES(?)", listOf(date))
            val result = executePreparedStatement(conn, "SELECT T FROM TEST WHERE T  = ?", listOf(date))
            assertThat(result.rows.size).isEqualTo(1)

            val dateTime = LocalDateTime.of(2016, 3, 5, 0, 0, 0, 0)
            val dateTimeResult = executePreparedStatement(conn, "SELECT T FROM TEST WHERE T  = ?", listOf(dateTime))
            assertThat(dateTimeResult.rows.size).isEqualTo(1)
        }
    }

    @Test
    fun `when processing times and dates should handle sending a LocalDateTime and return a LocalDateTime for a timestamp without timezone column`() {

        withHandler { conn ->
            val date1 = LocalDateTime.ofInstant(Instant.ofEpochMilli(2190319), ZoneOffset.UTC)

            awaitFuture(conn.sendPreparedStatement("CREATE TEMP TABLE TEST(T TIMESTAMP)"))
            awaitFuture(conn.sendPreparedStatement("INSERT INTO TEST(T) VALUES(?)", listOf(date1)))
            val result = awaitFuture(conn.sendPreparedStatement("SELECT T FROM TEST"))
            val date2 = (result.rows.head)(0)

            assertThat(date2).isEqualTo(date1)
        }
    }

    @Test
    fun `when processing times and dates should handle sending a date with timezone and retrieving the date , the same time zone`() {

        withHandler { conn ->
            val date1 = OffsetDateTime.ofInstant(Instant.ofEpochMilli(2190319), ZoneOffset.UTC)

            awaitFuture(conn.sendPreparedStatement("CREATE TEMP TABLE TEST(T TIMESTAMP WITH TIME ZONE)"))
            awaitFuture(conn.sendPreparedStatement("INSERT INTO TEST(T) VALUES(?)", listOf(date1)))
            val result = awaitFuture(conn.sendPreparedStatement("SELECT T FROM TEST"))
            val date2 = (result.rows.head)(0)

            assertThat(date2).isEqualTo(date1)
        }
    }

    @Test
    fun `when processing times and dates should support intervals`() {
        withHandler { handler ->

            executeDdl(handler, "CREATE TEMP TABLE intervals (duration interval NOT NULL)")

            val p = PeriodDuration.of(
                Period.of(1, 2, 4),
                Duration.ofHours(5).plusMinutes(6).plusSeconds(7).plusMillis(8)
            ) /* postgres normalizes weeks */

            executePreparedStatement(handler, "INSERT INTO intervals (duration) VALUES (?)", listOf(p))
            val rows = executeQuery(handler, "SELECT duration FROM intervals").rows

            assertThat(rows.length).isEqualTo(1)

            assertThat(rows(0)(0)).isEqualTo(p)
        }
    }
}
