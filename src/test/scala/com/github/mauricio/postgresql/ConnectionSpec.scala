package com.github.mauricio.postgresql

import org.specs2.mutable.Specification

/**
 * User: Maurício Linhares
 * Date: 2/25/12
 * Time: 8:12 PM
 */

class ConnectionSpec extends Specification {

  def doWithConnection[T](fn: (Connection) => T): T = {

    val connection = new Connection(
      host = "localhost",
      port = 5433,
      username = "postgres",
      password = "postgres",
      database = "postgres")

    connection.connect

    try {
      fn(connection)
    } finally {
      connection.disconnect
    }
  }

  "connection" should {

    "correctly coonect to the database" in {

      doWithConnection {
        connection =>

          Thread.sleep(1000)

          val s = connection.parameterStatuses

          List(
            s("application_name") === Connection.Name
          )
      }

    }

  }

}
