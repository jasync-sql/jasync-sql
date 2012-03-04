package com.github.mauricio.postgresql

import org.specs2.mutable.Specification
import java.util.concurrent.{TimeUnit, Future}

/**
 * User: Maurício Linhares
 * Date: 3/1/12
 * Time: 12:38 AM
 */

class DatabaseConnectionHandlerSpec extends Specification {

  def withHandler[T]( fn : (DatabaseConnectionHandler, Future[Map[String,String]]) => T ) : T = {

    val handler = new DatabaseConnectionHandler( "localhost", 5433, "postgres", "postgres" )

    try {
      val future = handler.connect
      fn(handler, future)
    } finally {
      handler.disconnect
    }

  }


  "handler" should {

    "connect to the database" in {

      withHandler {
        (handler, future) =>

          future.get(5, TimeUnit.SECONDS)

          handler.isReadyForQuery must beTrue
      }

    }

    "query the database" in {

      withHandler {
        (handler, future) =>

          future.get(5, TimeUnit.SECONDS)

          val query = """create temp table type_test_table (
            bigserial_column bigserial not null,
            smallint_column smallint not null,
            integer_column integer not null,
            decimal_column decimal(6,3),
            numeric_column numeric(10,10),
            real_column real,
            double_column double precision,
            serial_column serial not null,
            varchar_column varchar(255),
            char_column varchar(255),
            text_column text,
            timestamp_column timestamp,
            date_column date,
            time_column time,
            boolean_column boolean,
            constraint bigserial_column_pkey primary key (bigserial_column)
          )"""

          handler.sendQuery( query )

          true === false
      }

    }

  }

}
