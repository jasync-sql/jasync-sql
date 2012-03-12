package com.github.mauricio.postgresql

import column.{TimeEncoderDecoder, DateEncoderDecoder, TimestampEncoderDecoder}
import org.specs2.mutable.Specification
import java.util.concurrent.TimeUnit

/**
 * User: Maurício Linhares
 * Date: 3/1/12
 * Time: 12:38 AM
 */

class DatabaseConnectionHandlerSpec extends Specification {

  val create = """create temp table type_test_table (
            bigserial_column bigserial not null,
            smallint_column smallint not null,
            integer_column integer not null,
            decimal_column decimal(10,4),
            real_column real,
            double_column double precision,
            serial_column serial not null,
            varchar_column varchar(255),
            text_column text,
            timestamp_column timestamp,
            date_column date,
            time_column time,
            boolean_column boolean,
            constraint bigserial_column_pkey primary key (bigserial_column)
          )"""

  val insert = """insert into type_test_table (
            smallint_column,
            integer_column,
            decimal_column,
            real_column,
            double_column,
            varchar_column,
            text_column,
            timestamp_column,
            date_column,
            time_column,
            boolean_column
            )
            VALUES (
            10,
            11,
            14.999,
            78.34,
            15.68,
            'this is a varchar field',
            'this is a long text field',
            '1984-08-06 22:13:45.888888',
            '1984-08-06',
            '22:13:45.888888',
            TRUE
            )
            """

  val select = "select * from type_test_table"

  val preparedStatementCreate = """create temp table prepared_statement_test (
    id bigserial not null,
    name varchar(255) not null,
    constraint bigserial_column_pkey primary key (id)
  )"""

  val preparedStatementInsert = " insert into prepared_statement_test (name) values ('John Doe')"
  val preparedStatementSelect = "select * from prepared_statement_test"

  def withHandler[T]( fn : (DatabaseConnectionHandler) => T ) : T = {

    val handler = new DatabaseConnectionHandler( "localhost", 5433, "postgres", "netty_driver_test" )

    try {
      handler.connect.get
      fn(handler)
    } finally {
      handler.disconnect
    }

  }


  "handler" should {

    "connect to the database" in {

      withHandler {
        handler =>
          handler.isReadyForQuery must beTrue
      }

    }

    "create a table in the database" in {

      withHandler {
        handler =>
          handler.sendQuery( this.create ).get( 5, TimeUnit.SECONDS ).rowsAffected === 0
      }

    }

    "insert a row in the database" in {

      withHandler {
        handler =>
          handler.sendQuery( this.create ).get( 5, TimeUnit.SECONDS )
          handler.sendQuery( this.insert ).get( 5, TimeUnit.SECONDS ).rowsAffected === 1
      }

    }

    "select rows in the database" in {

      withHandler {
        handler =>
          handler.sendQuery( this.create ).get( 5, TimeUnit.SECONDS)
          handler.sendQuery( this.insert ).get( 5, TimeUnit.SECONDS )
          val queryResult = handler.sendQuery( this.select ).get( 5, TimeUnit.SECONDS )
          val rows = queryResult.rows.get

          List(
            rows(0,0) === 1,
            rows(1,0) === 10,
            rows(2,0) === 11,
            rows(3,0) === 14.999,
            rows(4,0).toString === 78.34.toString,
            rows(5,0) === 15.68,
            rows(6,0) === 1,
            rows(7,0) === "this is a varchar field",
            rows(8,0) === "this is a long text field",
            rows(9,0) === TimestampEncoderDecoder.decode("1984-08-06 22:13:45.888888"),
            rows(10,0) === DateEncoderDecoder.decode("1984-08-06"),
            rows(11,0) === TimeEncoderDecoder.decode("22:13:45.888888"),
            rows(12,0) === true
          )
      }

    }

    "execute a prepared statement" in {

      withHandler {
        handler =>
          handler.sendQuery(this.preparedStatementCreate).get(5, TimeUnit.SECONDS)
          handler.sendQuery(this.preparedStatementInsert).get(5, TimeUnit.SECONDS)
          val queryResult = handler.sendPreparedStatement( this.preparedStatementSelect ).get(5, TimeUnit.SECONDS)
          val rows = queryResult.rows.get

          List(
            rows(0,0) === 1,
            rows(1,0) === "John Doe"
          )
      }

    }

  }

}
