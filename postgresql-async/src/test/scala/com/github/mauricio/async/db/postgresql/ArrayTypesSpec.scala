/*
 * Copyright 2013 Maurício Linhares
 *
 * Maurício Linhares licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.github.mauricio.async.db.postgresql

import com.github.mauricio.async.db.column.TimestampWithTimezoneEncoderDecoder
import org.specs2.mutable.Specification

class ArrayTypesSpec extends Specification with DatabaseTestHelper {

  val simpleCreate = """create temp table type_test_table (
            bigserial_column bigserial not null,
            smallint_column integer[] not null,
            text_column text[] not null,
            timestamp_column timestamp with time zone[] not null,
            constraint bigserial_column_pkey primary key (bigserial_column)
          )"""

  val insert =
    """insert into type_test_table
      (smallint_column, text_column, timestamp_column)
      values (
      '{1,2,3,4}',
      '{"some,\"comma,separated,text","another line of text","fake\,backslash","real\\,backslash\\",NULL}',
      '{"2013-04-06 01:15:10.528-03","2013-04-06 01:15:08.528-03"}'
      )"""

  val insertPreparedStatement = """insert into type_test_table
            (smallint_column, text_column, timestamp_column)
            values (?,?,?)"""

  "connection" should {

    "correctly parse the array type" in {

      withHandler {
        handler =>
          executeDdl(handler, simpleCreate)
          executeDdl(handler, insert, 1)
          val result = executeQuery(handler, "select * from type_test_table").rows.get
          result(0)("smallint_column") === List(1,2,3,4)
          result(0)("text_column") === List("some,\"comma,separated,text", "another line of text", "fake,backslash", "real\\,backslash\\", null )
          result(0)("timestamp_column") === List(
            TimestampWithTimezoneEncoderDecoder.decode("2013-04-06 01:15:10.528-03"),
            TimestampWithTimezoneEncoderDecoder.decode("2013-04-06 01:15:08.528-03")
          )
      }

    }

    "correctly send arrays using prepared statements" in {

      val timestamps = List(
        TimestampWithTimezoneEncoderDecoder.decode("2013-04-06 01:15:10.528-03"),
        TimestampWithTimezoneEncoderDecoder.decode("2013-04-06 01:15:08.528-03")
      )
      val numbers = List(1,2,3,4)
      val texts = List("some,\"comma,separated,text", "another line of text", "fake,backslash", "real\\,backslash\\", null )

      withHandler {
        handler =>
          executeDdl(handler, simpleCreate)
          executePreparedStatement(
            handler,
            this.insertPreparedStatement,
            Array( numbers, texts, timestamps ) )

          val result = executeQuery(handler, "select * from type_test_table").rows.get

          result(0)("smallint_column") === numbers
          result(0)("text_column") === texts
          result(0)("timestamp_column") === timestamps
      }

    }

  }

}
