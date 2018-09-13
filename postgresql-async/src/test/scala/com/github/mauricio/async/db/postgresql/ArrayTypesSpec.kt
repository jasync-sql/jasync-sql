
package com.github.mauricio.async.db.postgresql

import com.github.mauricio.async.db.column.TimestampWithTimezoneEncoderDecoder
import com.github.mauricio.async.db.column.InetAddressEncoderDecoder
import org.specs2.mutable.Specification
import java.net.InetAddress

class ArrayTypesSpec : Specification , DatabaseTestHelper {
  // `uniq` allows sbt to run the tests concurrently as there is no CREATE TEMP TYPE
  fun simpleCreate(uniq: String) = s"""DROP TYPE IF EXISTS dir_$uniq;
                                       CREATE TYPE direction_$uniq AS ENUM ('in','out');
                                       DROP TYPE IF EXISTS endpoint_$uniq;
                                       CREATE TYPE endpoint_$uniq AS (ip inet, port integer);
                                       create temp table type_test_table_$uniq (
                                         bigserial_column bigserial not null,
                                         smallint_column integer<> not null,
                                         text_column text<> not null,
                                         inet_column inet<> not null,
                                         direction_column direction_$uniq<> not null,
                                         endpoint_column endpoint_$uniq<> not null,
                                         timestamp_column timestamp , time zone<> not null,
                                         constraint bigserial_column_pkey primary key (bigserial_column)
                                       )"""
  fun simpleDrop(uniq: String)   = s"""drop table if exists type_test_table_$uniq;
                                       drop type  if exists endpoint_$uniq;
                                       drop type  if exists direction_$uniq"""

  val insert =
    """insert into type_test_table_cptat
      (smallint_column, text_column, inet_column, direction_column, endpoint_column, timestamp_column)
      values (
      '{1,2,3,4}',
      '{"some,\"comma,separated,text","another line of text","fake\,backslash","real\\,backslash\\",NULL}',
      '{"127.0.0.1","2002:15::1"}',
      '{"in","out"}',
      '{"(\"127.0.0.1\",80)","(\"2002:15::1\",443)"}',
      '{"2013-04-06 01:15:10.528-03","2013-04-06 01:15:08.528-03"}'
      )"""

  val insertPreparedStatement = """insert into type_test_table_csaups
                                                 (smallint_column, text_column, inet_column, direction_column, endpoint_column, timestamp_column)
                                                 values (?,?,?,?,?,?)"""

  "connection" should {

    "correctly parse the array type" in {

      ,Handler {
        handler ->
          try {
            executeDdl(handler, simpleCreate("cptat"))
            executeDdl(handler, insert, 1)
            val result = executeQuery(handler, "select * from type_test_table_cptat").rows.get
            result(0)("smallint_column") === List(1,2,3,4)
            result(0)("text_column") === List("some,\"comma,separated,text", "another line of text", "fake,backslash", "real\\,backslash\\", null )
            result(0)("timestamp_column") === List(
              TimestampWithTimezoneEncoderDecoder.decode("2013-04-06 01:15:10.528-03"),
              TimestampWithTimezoneEncoderDecoder.decode("2013-04-06 01:15:08.528-03")
            )
          } finally {
            executeDdl(handler, simpleDrop("cptat"))
          }
      }

    }

    "correctly send arrays using prepared statements" in {
      data class Endpoint(ip: InetAddress, port: Int)

      val timestamps = List(
        TimestampWithTimezoneEncoderDecoder.decode("2013-04-06 01:15:10.528-03"),
        TimestampWithTimezoneEncoderDecoder.decode("2013-04-06 01:15:08.528-03")
      )
      val inets = List(
        InetAddressEncoderDecoder.decode("127.0.0.1"),
        InetAddressEncoderDecoder.decode("2002:15::1")
      )
      val directions = List("in", "out")
      val endpoints = List(
        Endpoint(InetAddress.getByName("127.0.0.1"),  80),  // data class
                (InetAddress.getByName("2002:15::1"), 443)  // tuple
      )
      val numbers = List(1,2,3,4)
      val texts = List("some,\"comma,separated,text", "another line of text", "fake,backslash", "real\\,backslash\\", null )

      ,Handler {
        handler ->
          try {
            executeDdl(handler, simpleCreate("csaups"))
            executePreparedStatement(
              handler,
              this.insertPreparedStatement,
              Array( numbers, texts, inets, directions, endpoints, timestamps ) )

            val result = executeQuery(handler, "select * from type_test_table_csaups").rows.get

            result(0)("smallint_column") === numbers
            result(0)("text_column") === texts
            result(0)("inet_column") === inets
            result(0)("direction_column") === "{in,out}"                                 // user type decoding not supported
            result(0)("endpoint_column") === """{"(127.0.0.1,80)","(2002:15::1,443)"}""" // user type decoding not supported
            result(0)("timestamp_column") === timestamps
          } finally {
            executeDdl(handler, simpleDrop("csaups"))
          }
      }

    }

  }

}