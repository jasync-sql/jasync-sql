package com.github.jasync.sql.db.mysql

import org.junit.Test
import java.math.BigDecimal
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
            assertEquals(1, result.get(0)["id"])
            val otherResult = assertNotNull(executePreparedStatement(connection, "select 1 as id , 'joe' as name").rows)

            assertEquals(1, otherResult.size)
            assertEquals("joe", otherResult[0]["name"])
            assertEquals(1, otherResult[0]["id"])
        }
    }

    @Test
    fun `be able to detect a null value()a prepared statement`() {

        withConnection { connection ->
            val result = assertNotNull(executePreparedStatement(connection, "select 1 as id , 'joe' as name, NULL as null_value").rows)
            assertEquals(1, result.size)
            assertEquals("joe", result[0]["name"])
            assertEquals(1, result[0]["id"])
            assertNull(result[0]["null_value"])
        }

    }

    @Test
    fun `be able to select numbers and process them`() {

        withConnection { connection ->
            executeQuery(connection, createTableNumericColumns)
            executeQuery(connection, insertTableNumericColumns)
            val result = assertNotNull(assertNotNull(executePreparedStatement(connection, "SELECT * FROM numbers").rows)[0])

            assertEquals(-100, result["number_tinyint"] as Byte)
            assertEquals(32766, result["number_smallint"] as Short)
            assertEquals(8388607, result["number_mediumint"] as Int)
            assertEquals(2147483647, result["number_int"] as Int)
            assertEquals(9223372036854775807, result["number_bigint"] as Long)
            assertEquals(BigDecimal(450.764491), result["number_bigint"])
            assertEquals(14.7F, result["number_float"])
            assertEquals(87650.9876, result["number_double"])
        }

    }

    @Test
    fun `be able to select from a table with timestamps`() {

        withConnection { connection ->
            executeQuery(connection, createTableTimeColumns)
            executeQuery(connection, insertTableTimeColumns)
            val result = assertNotNull(assertNotNull(executePreparedStatement(connection, "SELECT * FROM posts").rows)[0])
            val date = result["created_at_date"] as org.joda.time.LocalDate

            assertEquals(2038, date.year)
            assertEquals(1, date.monthOfYear)
            assertEquals(19, date.dayOfMonth)

            val dateTime = result["created_at_datetime"] as org.joda.time.LocalDateTime

            assertEquals(2038, dateTime.year)
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

            //assertEquals(Duration(3, TimeUnit.HOURS) + Duration(14, TimeUnit.MINUTES) + Duration(7, TimeUnit.SECONDS), result["created_at_time"])

            val year = result["created_at_year"] as Short
            assertEquals(1999, year)
        }

    }

    @Test
    fun `it should be able to bind statement values to the prepared statement`() {

        withConnection { connection ->
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
            executePreparedStatement(connection,
                    insert,
                    listOf(
                            byte,
                            short,
                            mediumInt,
                            int,
                            bigInt,
                            bigDecimal,
                            float,
                            double)
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

    /*
    @Test
    fun `bind parameters on a prepared statement`(){

      val create = """CREATE TEMPORARY TABLE posts (
                     |       id INT NOT NULL AUTO_INCREMENT,
                     |       some_text TEXT not null,
                     |       primary key (id) )"""

      val insert = "insert into posts (some_text) values (?)"
      val select = "select * from posts"

      withConnection {
        connection ->
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

    @Test
    fun `bind timestamp parameters to a table`(){

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
        connection ->
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

    @Test
    fun `read a timestamp with microseconds`(){

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
        connection ->

          if ( connection.version < MySQLConnection.MicrosecondsVersion ) {
            true === true // no op
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

    @Test
    fun `support prepared statement with a big string`(){

      val bigString = {
        val builder = new StringBuilder()
        for (i <- 0 until 400)
          builder.append( "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789")
        builder.toString()
      }

      withConnection {
        connection ->
          executeQuery(connection, "CREATE TEMPORARY TABLE BIGSTRING( id INT NOT NULL AUTO_INCREMENT, STRING LONGTEXT, primary key (id))")
          executePreparedStatement(connection, "INSERT INTO BIGSTRING (STRING) VALUES (?)", bigString)
          val row = executePreparedStatement(connection, "SELECT STRING, id FROM BIGSTRING").rows.get(0)
          row("id") === 1
          val result = row("STRING").asInstanceOf[String]
          result === bigString
      }
    }

    @Test
    fun `support setting null to a column`(){
      withConnection {
        connection ->
          executeQuery(connection, "CREATE TEMPORARY TABLE timestamps ( id INT NOT NULL, moment TIMESTAMP NULL, primary key (id))")
          executePreparedStatement(connection, "INSERT INTO timestamps (moment, id) VALUES (?, ?)", null, 10)
          val row = executePreparedStatement(connection, "SELECT moment, id FROM timestamps").rows.get(0)
          row("id") === 10
          row("moment") === null
      }
    }

    @Test
    fun `support setting None to a column`(){
      withConnection {
        connection ->
          executeQuery(connection, "CREATE TEMPORARY TABLE timestamps ( id INT NOT NULL, moment TIMESTAMP NULL, primary key (id))")
          executePreparedStatement(connection, "INSERT INTO timestamps (moment, id) VALUES (?, ?)", None, 10)
          val row = executePreparedStatement(connection, "SELECT moment, id FROM timestamps").rows.get(0)
          row("id") === 10
          row("moment") === null
      }
    }

    @Test
    fun `support setting Some(value) to a column`(){
      withConnection {
        connection ->
          executeQuery(connection, "CREATE TEMPORARY TABLE timestamps ( id INT NOT NULL, moment TIMESTAMP NULL, primary key (id))")
          val moment = LocalDateTime.now().withMillisOfDay(0) // cut off millis to match timestamp
          executePreparedStatement(connection, "INSERT INTO timestamps (moment, id) VALUES (?, ?)", Some(moment), 10)
          val row = executePreparedStatement(connection, "SELECT moment, id FROM timestamps").rows.get(0)
          row("id") === 10
          row("moment") === moment
      }
    }

    @Test
    fun `bind parameters on a prepared statement with limit`(){

      val create = """CREATE TEMPORARY TABLE posts (
                     |       id INT NOT NULL AUTO_INCREMENT,
                     |       some_text TEXT not null,
                     |       some_date DATE,
                     |       primary key (id) )""".stripMargin

      val insert = "insert into posts (some_text) values (?)"
      val select = "select * from posts limit 100"

      withConnection {
        connection ->
          executeQuery(connection, create)

          executePreparedStatement(connection, insert, "this is some text here")

          val row = executeQuery(connection, select).rows.get(0)

          row("id") === 1
          row("some_text") === "this is some text here"
          row("some_date") must beNull

          val queryRow = executePreparedStatement(connection, select).rows.get(0)

          queryRow("id") === 1
          queryRow("some_text") === "this is some text here"
          queryRow("some_date") must beNull

      }
    }

    @Test
    fun `insert with prepared statements and without columns`(){
      withConnection {
        connection ->
          executeQuery(connection, this.createTable)

          executePreparedStatement(connection, this.insert)

          val result = executePreparedStatement(connection, this.select).rows.get
          result.size === 1

          result(0)("name") === "Maurício Aragão"
      }
    }
    */
}
