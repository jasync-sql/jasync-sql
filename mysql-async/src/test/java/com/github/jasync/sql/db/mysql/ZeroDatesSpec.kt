package com.github.jasync.sql.db.mysql

import com.github.jasync.sql.db.RowData
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Test

class ZeroDatesSpec : ConnectionHelper() {

    val createStatement = """
      CREATE TEMPORARY TABLE dates (
      `name` varchar (255) NOT NULL,
      `timestamp_column` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
      `date_column` date NOT NULL DEFAULT '0000-00-00',
      `datetime_column` datetime NOT NULL DEFAULT '0000-00-00 00:00:00',
      `time_column` time NOT NULL DEFAULT '00:00:00',
      `year_column` year NOT NULL DEFAULT '0000'
      )
      ENGINE=INNODB DEFAULT CHARSET=utf8;"""

    val insertStatement = "INSERT INTO dates (name) values ('Joe')"
    val selectStatement = "SELECT * FROM dates"

    fun matchValues(result: RowData) {
        assertEquals("Joe", result["name"])
        assertNull(result["timestamp_column"])
        assertNull(result["datetime_column"])
        assertNull(result["date_column"])
        val zero: Short = 0
        assertEquals(zero, result["year_column"])
        assertEquals(java.time.Duration.ZERO, result["time_column"])
    }

    @Test
    fun `correctly parse the MySQL zeroed dates as NULL values in text protocol`() {

        withConnection { connection ->
            executeQuery(
                connection,
                "set SESSION sql_mode = ''"
            ) //  https://stackoverflow.com/questions/15701636/how-to-enable-explicit-defaults-for-timestamp
            executeQuery(connection, createStatement)
            executeQuery(connection, insertStatement)
            matchValues(assertNotNull(executeQuery(connection, selectStatement).rows)[0])
        }
    }

    @Test
    fun `correctly parse the MySQL zeroed dates as NULL values in binary protocol`() {

        withConnection { connection ->
            executeQuery(
                connection,
                "set SESSION sql_mode = ''"
            ) //  https://stackoverflow.com/questions/15701636/how-to-enable-explicit-defaults-for-timestamp
            executeQuery(connection, createStatement)
            executeQuery(connection, insertStatement)

            matchValues(assertNotNull(executePreparedStatement(connection, selectStatement).rows)[0])
        }
    }
}
