package com.github.jasync.sql.db.mysql

class QueryTimeoutSpec : ConnectionHelper() {
  /*
  implicit def unitAsResult: AsResult[Unit] = new AsResult[Unit] {
    def asResult(r: =>Unit) =
      ResultExecution.execute(r)(_ => Success())
  }
  "Simple query with 1 nanosec timeout" in {
    withConfigurablePool(shortTimeoutConfiguration) {
      pool => {
        val connection = Await.result(pool.take, Duration(10,SECONDS))
        connection.isTimeouted === false
        connection.isConnected === true
        val queryResultFuture = connection.sendQuery("select sleep(1)")
        Await.result(queryResultFuture, Duration(10,SECONDS)) must throwA[TimeoutException]()
        connection.isTimeouted === true
        Await.ready(pool.giveBack(connection), Duration(10,SECONDS))
        pool.availables.count(_ == connection) === 0 // connection removed from pool
        // we do not know when the connection will be closed.
      }
    }
  }

  "Simple query with 5 sec timeout" in {
    withConfigurablePool(longTimeoutConfiguration) {
      pool => {
        val connection = Await.result(pool.take, Duration(10,SECONDS))
        connection.isTimeouted === false
        connection.isConnected === true
        val queryResultFuture = connection.sendQuery("select sleep(1)")
        Await.result(queryResultFuture, Duration(10,SECONDS)).rows.get.size === 1
        connection.isTimeouted === false
        connection.isConnected === true
        Await.ready(pool.giveBack(connection), Duration(10,SECONDS))
        pool.availables.count(_ == connection) === 1 // connection returned to pool
      }
    }
  }

  def shortTimeoutConfiguration = new Configuration(
    "mysql_async",
    "localhost",
    port = 3306,
    password = Some("root"),
    database = Some("mysql_async_tests"),
    queryTimeout = Some(Duration(1,NANOSECONDS))
  )

  def longTimeoutConfiguration = new Configuration(
    "mysql_async",
    "localhost",
    port = 3306,
    password = Some("root"),
    database = Some("mysql_async_tests"),
    queryTimeout = Some(Duration(5,SECONDS))
  )
  */
}
