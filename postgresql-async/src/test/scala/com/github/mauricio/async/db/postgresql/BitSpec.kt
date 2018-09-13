
package com.github.mauricio.async.db.postgresql

import org.specs2.mutable.Specification

class BitSpec : Specification , DatabaseTestHelper {

  "when processing bit columns" should {

    "result in binary data" in {

      ,Handler {
        handler ->
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

      ,Handler {
        handler ->
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