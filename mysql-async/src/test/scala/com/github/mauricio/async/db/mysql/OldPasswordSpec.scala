package com.github.mauricio.async.db.mysql

import org.specs2.mutable.Specification
import com.github.mauricio.async.db.Configuration
import com.github.mauricio.async.db.util.FutureUtils.awaitFuture
import com.github.mauricio.async.db.mysql.exceptions.MySQLException

class OldPasswordSpec extends Specification with ConnectionHelper {

  "connection" should {

    "connect and query the database" in {
      val connection = new MySQLConnection(defaultConfiguration)
      try {
        awaitFuture(connection.connect)
        success
      } catch {
        case e : MySQLException => {
          (e.errorMessage.errorCode === 1275).orSkip
          success
        }
      }
    }

  }

  override def defaultConfiguration = new Configuration(
    "mysql_async_old",
    "localhost",
    port = 3306,
    password = Some("do_not_use_this"),
    database = Some("mysql_async_tests")
  )

}
