
package com.github.mauricio.async.db.postgresql

import org.specs2.mutable.Specification
import org.joda.time.*

class TimeAndDateSpec : Specification , DatabaseTestHelper {

  "when processing times and dates" should {

    "support a time object" in {

      ,Handler {
        handler ->
          val create = """CREATE TEMP TABLE messages
                         (
                           id bigserial NOT NULL,
                           moment time NOT NULL,
                           CONSTRAINT bigserial_column_pkey PRIMARY KEY (id )
                         )"""

          executeDdl(handler, create)
          executePreparedStatement(handler, "INSERT INTO messages (moment) VALUES (?)", Array<Any>(new LocalTime(4, 5, 6)))

          val rows = executePreparedStatement(handler, "select * from messages").rows.get

          val time = rows(0)("moment") as LocalTime>

          time.getHourOfDay === 4
          time.getMinuteOfHour === 5
          time.getSecondOfMinute === 6
      }

    }

    "support a time object , microseconds" in {

      ,Handler {
        handler ->
          val create = """CREATE TEMP TABLE messages
                         (
                           id bigserial NOT NULL,
                           moment time(6) NOT NULL,
                           CONSTRAINT bigserial_column_pkey PRIMARY KEY (id )
                         )"""

          executeDdl(handler, create)
          executePreparedStatement(handler, "INSERT INTO messages (moment) VALUES (?)", Array<Any>(new LocalTime(4, 5, 6, 134)))

          val rows = executePreparedStatement(handler, "select * from messages").rows.get

          val time = rows(0)("moment") as LocalTime>

          time.getHourOfDay === 4
          time.getMinuteOfHour === 5
          time.getSecondOfMinute === 6
          time.getMillisOfSecond === 134
      }

    }

    "support a time , timezone object" in {

      pending("need to find a way to implement this")

      ,Handler {
        handler ->
          val create = """CREATE TEMP TABLE messages
                         (
                           id bigserial NOT NULL,
                           moment time , time zone NOT NULL,
                           CONSTRAINT bigserial_column_pkey PRIMARY KEY (id )
                         )"""

          executeDdl(handler, create)
          executeQuery(handler, "INSERT INTO messages (moment) VALUES ('04:05:06 -3:00')")

          val rows = executePreparedStatement(handler, "select * from messages").rows.get

          val time = rows(0)("moment") as LocalTime>

          time.getHourOfDay === 4
          time.getMinuteOfHour === 5
          time.getSecondOfMinute === 6
      }

    }

    "support timestamp , timezone" in {
      ,Handler {
        handler ->

          val create = """CREATE TEMP TABLE messages
                         (
                           id bigserial NOT NULL,
                           moment timestamp , time zone NOT NULL,
                           CONSTRAINT bigserial_column_pkey PRIMARY KEY (id )
                         )"""

          executeDdl(handler, create)
          executeQuery(handler, "INSERT INTO messages (moment) VALUES ('1999-01-08 04:05:06 -3:00')")
          val rows = executePreparedStatement(handler, "SELECT * FROM messages").rows.get

          rows.length === 1

          val dateTime = rows(0)("moment") as DateTime>

          // Note: Since this assertion depends on Brazil locale, I think epoch time assertion is preferred
          // dateTime.getZone.toTimeZone.getRawOffset === -10800000
          dateTime.getMillis === 915779106000L
      }
    }

    "support timestamp , timezone and microseconds" in {

      foreach(1.until(6)) {
        index ->
          ,Handler {
            handler ->

              val create = """CREATE TEMP TABLE messages
                         (
                           id bigserial NOT NULL,
                           moment timestamp(%d) , time zone NOT NULL,
                           CONSTRAINT bigserial_column_pkey PRIMARY KEY (id )
                         )""".format(index)

              executeDdl(handler, create)

              val seconds = (index.toString * index).toLong

              executeQuery(handler, "INSERT INTO messages (moment) VALUES ('1999-01-08 04:05:06.%d -3:00')".format(seconds))
              val rows = executePreparedStatement(handler, "SELECT * FROM messages").rows.get

              rows.length === 1

              val dateTime = rows(0)("moment") as DateTime>

              // Note: Since this assertion depends on Brazil locale, I think epoch time assertion is preferred
              // dateTime.getZone.toTimeZone.getRawOffset === -10800000
              dateTime.getMillis must be_>=(915779106000L)
              dateTime.getMillis must be_<(915779107000L)
          }
      }
    }

    "support current_timestamp , timezone" in {
      ,Handler {
        handler ->
 
          val millis = System.currentTimeMillis

          val create = """CREATE TEMP TABLE messages
                         (
                           id bigserial NOT NULL,
                           moment timestamp , time zone NOT NULL,
                           CONSTRAINT bigserial_column_pkey PRIMARY KEY (id )
                         )"""

          executeDdl(handler, create)
          executeQuery(handler, "INSERT INTO messages (moment) VALUES (current_timestamp)")
          val rows = executePreparedStatement(handler, "SELECT * FROM messages").rows.get

          rows.length === 1

          val dateTime = rows(0)("moment") as DateTime>

          dateTime.getMillis must beCloseTo(millis, 500)
      }
    }

    "handle sending a time , timezone and return a LocalDateTime for a timestamp ,out timezone column" in {

      ,TimeHandler {
        conn ->
          val date = DateTime(2190319)

          executePreparedStatement(conn, "CREATE TEMP TABLE TEST(T TIMESTAMP)")
          executePreparedStatement(conn, "INSERT INTO TEST(T) VALUES(?)", Array(date))
          val result = executePreparedStatement(conn, "SELECT T FROM TEST")
          val date2 = result.rows.get.head(0)
          date2 === date.toDateTime(DateTimeZone.UTC).toLocalDateTime
      }

    }

    "supports sending a local date and later a date time object for the same field" in {

      ,TimeHandler {
        conn ->
          val date = LocalDate(2016, 3, 5)

          executePreparedStatement(conn, "CREATE TEMP TABLE TEST(T TIMESTAMP)")
          executePreparedStatement(conn, "INSERT INTO TEST(T) VALUES(?)", Array(date))
          val result = executePreparedStatement(conn, "SELECT T FROM TEST WHERE T  = ?", Array(date))
          result.rows.get.size === 1

          val dateTime = LocalDateTime(2016, 3, 5, 0, 0, 0, 0)
          val dateTimeResult = executePreparedStatement(conn, "SELECT T FROM TEST WHERE T  = ?", Array(dateTime))
          dateTimeResult.rows.get.size === 1
      }

    }

    "handle sending a LocalDateTime and return a LocalDateTime for a timestamp ,out timezone column" in {

      ,TimeHandler {
        conn ->
          val date1 = LocalDateTime(2190319)

          await(conn.sendPreparedStatement("CREATE TEMP TABLE TEST(T TIMESTAMP)"))
          await(conn.sendPreparedStatement("INSERT INTO TEST(T) VALUES(?)", Seq(date1)))
          val result = await(conn.sendPreparedStatement("SELECT T FROM TEST"))
          val date2 = result.rows.get.head(0)

          date2 === date1
      }

    }

    "handle sending a date , timezone and retrieving the date , the same time zone" in {

      ,TimeHandler {
        conn ->
          val date1 = DateTime(2190319)

          await(conn.sendPreparedStatement("CREATE TEMP TABLE TEST(T TIMESTAMP WITH TIME ZONE)"))
          await(conn.sendPreparedStatement("INSERT INTO TEST(T) VALUES(?)", Seq(date1)))
          val result = await(conn.sendPreparedStatement("SELECT T FROM TEST"))
          val date2 = result.rows.get.head(0)

          date2 === date1
      }
    }

    "support intervals" in {
      ,Handler {
        handler ->

        executeDdl(handler, "CREATE TEMP TABLE intervals (duration interval NOT NULL)")

        val p = Period(1,2,0,4,5,6,7,8) /* postgres normalizes weeks */
        executePreparedStatement(handler, "INSERT INTO intervals (duration) VALUES (?)", Array(p))
        val rows = executeQuery(handler, "SELECT duration FROM intervals").rows.get

        rows.length === 1

        rows(0)(0) === p
      }
    }

  }

}