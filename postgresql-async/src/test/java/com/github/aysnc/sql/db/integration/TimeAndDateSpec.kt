package com.github.aysnc.sql.db.integration

import com.github.jasync.sql.db.invoke
import com.github.jasync.sql.db.util.head
import com.github.jasync.sql.db.util.length
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime
import org.joda.time.LocalTime
import org.joda.time.Period
import org.junit.Ignore
import org.junit.Test

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
            executePreparedStatement(handler, "INSERT INTO messages (moment) VALUES (?)", listOf(LocalTime(4, 5, 6)))

            val rows = executePreparedStatement(handler, "select * from messages").rows

            val time = rows[0]("moment") as LocalTime

            assertThat(time.hourOfDay).isEqualTo(4)
            assertThat(time.minuteOfHour).isEqualTo(5)
            assertThat(time.secondOfMinute).isEqualTo(6)
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
                listOf(LocalTime(4, 5, 6, 134))
            )

            val rows = executePreparedStatement(handler, "select * from messages").rows

            val time = rows(0)("moment") as LocalTime

            assertThat(time.hourOfDay).isEqualTo(4)
            assertThat(time.minuteOfHour).isEqualTo(5)
            assertThat(time.secondOfMinute).isEqualTo(6)
            assertThat(time.millisOfSecond).isEqualTo(134)
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

            assertThat(time.hourOfDay).isEqualTo(4)
            assertThat(time.minuteOfHour).isEqualTo(5)
            assertThat(time.secondOfMinute).isEqualTo(6)
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

            val dateTime = rows(0)("moment") as DateTime

            // Note: Since this assertion depends on Brazil locale, I think epoch time assertion is preferred
            // assertThat(          // dateTime.getZone.toTimeZone.getRawOffset).isEqualTo(-10800000)
            assertThat(dateTime.millis).isEqualTo(915779106000L)
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

                val dateTime = rows(0)("moment") as DateTime

                // Note: Since this assertion depends on Brazil locale, I think epoch time assertion is preferred
                // dateTime.getZone.toTimeZone.getRawOffset).isEqualTo(-10800000)
                assertThat(dateTime.millis).isGreaterThan(915779106000L)
                assertThat(dateTime.millis).isLessThan(915779107000L)
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

            val dateTime = rows(0)("moment") as DateTime

            assertThat(dateTime.millis).isCloseTo(millis, Offset.offset(500L))
        }
    }

    @Test
    fun `when processing times and dates should handle sending a time with timezone and return a LocalDateTime for a timestamp without timezone column`() {

        withHandler { conn ->
            val date = DateTime(2190319)

            executePreparedStatement(conn, "CREATE TEMP TABLE TEST(T TIMESTAMP)")
            executePreparedStatement(conn, "INSERT INTO TEST(T) VALUES(?)", listOf(date))
            val result = executePreparedStatement(conn, "SELECT T FROM TEST")
            val date2 = (result.rows.head)(0)
            assertThat(date2).isEqualTo(date.toDateTime(DateTimeZone.UTC).toLocalDateTime())
        }
    }

    @Test
    fun `when processing times and dates should supports sending a local date and later a date time object for the same field`() {

        withHandler { conn ->
            val date = LocalDate(2016, 3, 5)

            executePreparedStatement(conn, "CREATE TEMP TABLE TEST(T TIMESTAMP)")
            executePreparedStatement(conn, "INSERT INTO TEST(T) VALUES(?)", listOf(date))
            val result = executePreparedStatement(conn, "SELECT T FROM TEST WHERE T  = ?", listOf(date))
            assertThat(result.rows.size).isEqualTo(1)

            val dateTime = LocalDateTime(2016, 3, 5, 0, 0, 0, 0)
            val dateTimeResult = executePreparedStatement(conn, "SELECT T FROM TEST WHERE T  = ?", listOf(dateTime))
            assertThat(dateTimeResult.rows.size).isEqualTo(1)
        }
    }

    @Test
    fun `when processing times and dates should handle sending a LocalDateTime and return a LocalDateTime for a timestamp without timezone column`() {

        withHandler { conn ->
            val date1 = LocalDateTime(2190319)

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
            val date1 = DateTime(2190319)

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

            val p = Period(1, 2, 0, 4, 5, 6, 7, 8) /* postgres normalizes weeks */
            executePreparedStatement(handler, "INSERT INTO intervals (duration) VALUES (?)", listOf(p))
            val rows = executeQuery(handler, "SELECT duration FROM intervals").rows

            assertThat(rows.length).isEqualTo(1)

            assertThat(rows(0)(0)).isEqualTo(p)
        }
    }
}
