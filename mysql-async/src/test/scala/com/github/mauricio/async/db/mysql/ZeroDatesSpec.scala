package com.github.mauricio.async.db.mysql

import org.specs2.mutable.Specification
import scala.concurrent.duration.Duration
import com.github.mauricio.async.db.RowData

class ZeroDatesSpec extends Specification with ConnectionHelper {

  val createStatement =
    """CREATE TEMPORARY TABLE dates (
      |`name` varchar (255) NOT NULL,
      |`timestamp_column` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
      |`date_column` date NOT NULL DEFAULT '0000-00-00',
      |`datetime_column` datetime NOT NULL DEFAULT '0000-00-00 00:00:00',
      |`time_column` time NOT NULL DEFAULT '00:00:00',
      |`year_column` year NOT NULL DEFAULT '0000'
      |)
      |ENGINE=MyISAM DEFAULT CHARSET=utf8;""".stripMargin

  val insertStatement = "INSERT INTO dates (name) values ('Joe')"
  val selectStatement = "SELECT * FROM dates"

  def matchValues( result : RowData ) = {
    result("name") === "Joe"
    result("timestamp_column") must beNull
    result("datetime_column") must beNull
    result("date_column") must beNull
    result("year_column") === 0
    result("time_column") === Duration.Zero
  }

  "client" should {

    "correctly parse the MySQL zeroed dates as NULL values in text protocol" in {

      withConnection {
        connection =>
          executeQuery(connection, createStatement)
          executeQuery(connection, insertStatement)

          matchValues(executeQuery(connection, selectStatement).rows.get(0))
      }
    }

    "correctly parse the MySQL zeroed dates as NULL values in binary protocol" in {

      withConnection {
        connection =>
          executeQuery(connection, createStatement)
          executeQuery(connection, insertStatement)

          matchValues(executePreparedStatement(connection, selectStatement).rows.get(0))
      }
    }

  }

}
