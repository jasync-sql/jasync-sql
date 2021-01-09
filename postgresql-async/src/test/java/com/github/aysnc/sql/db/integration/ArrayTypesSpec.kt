package com.github.aysnc.sql.db.integration

import com.github.jasync.sql.db.column.InetAddressEncoderDecoder
import com.github.jasync.sql.db.column.TimestampWithTimezoneEncoderDecoder
import com.github.jasync.sql.db.invoke
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ArrayTypesSpec : DatabaseTestHelper() {
    // `uniq` allows sbt to run the tests concurrently as there is no CREATE TEMP TYPE
    fun simpleCreate(uniq: String) = """DROP TYPE IF EXISTS dir_$uniq;
                                       CREATE TYPE direction_$uniq AS ENUM ('in','out');
                                       DROP TYPE IF EXISTS endpoint_$uniq;
                                       CREATE TYPE endpoint_$uniq AS (ip inet, port integer);
                                       create temp table type_test_table_$uniq (
                                         bigserial_column bigserial not null,
                                         smallint_column integer[] not null,
                                         text_column text[] not null,
                                         inet_column inet[] not null,
                                         direction_column direction_$uniq[] not null,
                                         endpoint_column endpoint_$uniq[] not null,
                                         timestamp_column timestamp with time zone[] not null,
                                         constraint bigserial_column_pkey primary key (bigserial_column)
                                       )"""

    fun simpleDrop(uniq: String) = """drop table if exists type_test_table_$uniq;
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

    @Test
    fun `connection should correctly parse the array type`() {

        withHandler { handler ->
            try {
                executeDdl(handler, simpleCreate("cptat"))
                executeDdl(handler, insert, 1)
                val result = executeQuery(handler, "select * from type_test_table_cptat").rows
                assertThat(result[0]("smallint_column")).isEqualTo(listOf(1, 2, 3, 4))
                assertThat(result[0]("text_column")).isEqualTo(
                    listOf(
                        "some,\"comma,separated,text",
                        "another line of text",
                        "fake,backslash",
                        "real\\,backslash\\",
                        null
                    )
                )
                assertThat(result[0]("timestamp_column")).isEqualTo(
                    listOf(
                        TimestampWithTimezoneEncoderDecoder.decode("2013-04-06 01:15:10.528-03"),
                        TimestampWithTimezoneEncoderDecoder.decode("2013-04-06 01:15:08.528-03")
                    )
                )
            } finally {
                executeDdl(handler, simpleDrop("cptat"))
            }
        }
    }

    @Test
    fun `connection should correctly send arrays using prepared statements`() {

        val timestamps = listOf(
            TimestampWithTimezoneEncoderDecoder.decode("2013-04-06 01:15:10.528-03"),
            TimestampWithTimezoneEncoderDecoder.decode("2013-04-06 01:15:08.528-03")
        )
        val inets = listOf(
            InetAddressEncoderDecoder.decode("127.0.0.1"),
            InetAddressEncoderDecoder.decode("2002:15::1")
        )
        val directions = listOf("in", "out")
        val endpoints = listOf(
            ("127.0.0.1" to 80), // data class
            ("2002:15::1" to 443) // tuple
        )
        val numbers = listOf(1, 2, 3, 4)
        val texts =
            listOf("some,\"comma,separated,text", "another line of text", "fake,backslash", "real\\,backslash\\", null)

        withHandler { handler ->
            try {
                executeDdl(handler, simpleCreate("csaups"))
                executePreparedStatement(
                    handler,
                    this.insertPreparedStatement,
                    listOf(numbers, texts, inets, directions, endpoints, timestamps)
                )

                val result = executeQuery(handler, "select * from type_test_table_csaups").rows

                assertThat(result[0]("smallint_column")).isEqualTo(numbers)
                assertThat(result[0]("text_column")).isEqualTo(texts)
                assertThat(result[0]("inet_column")).isEqualTo(inets)
                assertThat(result[0]("direction_column")).isEqualTo("{in,out}") // user type decoding not supported)
                assertThat(result[0]("endpoint_column")).isEqualTo("""{"(127.0.0.1,80)","(2002:15::1,443)"}""") // user type decoding not supported)
                assertThat(result[0]("timestamp_column")).isEqualTo(timestamps)
            } finally {
                executeDdl(handler, simpleDrop("csaups"))
            }
        }
    }
}
