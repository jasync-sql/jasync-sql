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

import org.specs2.mutable.Specification

class BitSpec extends Specification with DatabaseTestHelper {

  "when processing bit columns" should {

    "result in binary data" in {

      withHandler {
        handler =>
          val create = """CREATE TEMP TABLE binary_test
                         (
                           id bigserial NOT NULL,
                           some_bit BYTEA NOT NULL,
                           CONSTRAINT bigserial_column_pkey PRIMARY KEY (id)
                         )"""

          executeDdl(handler, create)
          executePreparedStatement(handler,
            "INSERT INTO binary_test (some_bit) VALUES (E'\\\\000'),(E'\\\\001')")

          val rows = executePreparedStatement(handler, "select * from binary_test").rows.get

          val bit0 = rows(0)("some_bit")
          val bit1 = rows(1)("some_bit")

          bit0 === Array(0)
          bit1 === Array(1)
      }

    }

    "result in binary data in BIT(2) column" in {

      withHandler {
        handler =>
          val create = """CREATE TEMP TABLE binary_test
                         (
                           id bigserial NOT NULL,
                           some_bit BYTEA NOT NULL,
                           CONSTRAINT bigserial_column_pkey PRIMARY KEY (id)
                         )"""

          executeDdl(handler, create)
          executePreparedStatement(handler,
            "INSERT INTO binary_test (some_bit) VALUES (E'\\\\000'),(E'\\\\001'),(E'\\\\002'),(E'\\\\003')")

          val rows = executePreparedStatement(handler, "select * from binary_test").rows.get

          val bit0 = rows(0)("some_bit")
          val bit1 = rows(1)("some_bit")
          val bit2 = rows(2)("some_bit")
          val bit3 = rows(3)("some_bit")

          bit0 === Array(0)
          bit1 === Array(1)
          bit2 === Array(2)
          bit3 === Array(3)
      }

    }

  }

}