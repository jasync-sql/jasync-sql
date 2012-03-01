package com.github.mauricio.postgresql

import org.specs2.mutable.Specification

/**
 * User: Maurício Linhares
 * Date: 3/1/12
 * Time: 12:38 AM
 */

class DatabaseConnectionHandlerSpec extends Specification {

  def withHandler[T]( fn : DatabaseConnectionHandler => T ) : T = {

    val handler = new DatabaseConnectionHandler( "localhost", 5433, "postgres", "postgres" )

    try {
      handler.connect
      fn(handler)
    } finally {
      handler.disconnect
    }

  }


  "handler" should {

    "connect to the database" in {

      withHandler {
        handler =>
          var tries = 0

          do {
            Thread.sleep(1000)
            tries += 1
          } while ( handler.isReadyForQuery && tries < 3 )


          handler.isReadyForQuery must beTrue
      }

    }

    "query the database" in {

      withHandler {
        handler =>
          handler.sendQuery( "select 10 as result" )

          Thread.sleep(4000)

          true === false
      }

    }

  }

}
