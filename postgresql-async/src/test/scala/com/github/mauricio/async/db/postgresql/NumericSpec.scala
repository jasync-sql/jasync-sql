package com.github.mauricio.async.db.postgresql

import org.specs2.mutable.Specification

class NumericSpec extends Specification with DatabaseTestHelper {

  "when processing numeric columns" should {

    "support first update of num column with floating" in {

      withHandler {
        handler =>
          executeDdl(handler, "CREATE TEMP TABLE numeric_test (id BIGSERIAL, numcol NUMERIC)")

          val id = executePreparedStatement(handler, "INSERT INTO numeric_test DEFAULT VALUES RETURNING id").rows.get(0)("id")
          executePreparedStatement(handler, "UPDATE numeric_test SET numcol = ? WHERE id = ?", Array[Any](123.123, id))
          executePreparedStatement(handler, "UPDATE numeric_test SET numcol = ? WHERE id = ?", Array[Any](1234, id))
          executePreparedStatement(handler, "UPDATE numeric_test SET numcol = ? WHERE id = ?", Array[Any](123.123, id))

          id === 1
      }

    }

    "support first update of num column with integer" in {

      withHandler {
        handler =>
          executeDdl(handler, "CREATE TEMP TABLE numeric_test (id BIGSERIAL, numcol NUMERIC)")

          val id = executePreparedStatement(handler, "INSERT INTO numeric_test DEFAULT VALUES RETURNING id").rows.get(0)("id")
          executePreparedStatement(handler, "UPDATE numeric_test SET numcol = ? WHERE id = ?", Array[Any](1234, id))
          executePreparedStatement(handler, "UPDATE numeric_test SET numcol = ? WHERE id = ?", Array[Any](123.123, id))

          id === 1
      }

    }

    "support using first update with queries instead of prepared statements" in {

      withHandler {
        handler =>
          executeDdl(handler, "CREATE TEMP TABLE numeric_test (id BIGSERIAL, numcol NUMERIC)")

          val id = executeQuery(handler, "INSERT INTO numeric_test DEFAULT VALUES RETURNING id").rows.get(0)("id")
          executeQuery(handler, s"UPDATE numeric_test SET numcol = 1234 WHERE id = $id")
          executeQuery(handler, s"UPDATE numeric_test SET numcol = 123.123 WHERE id = $id")

          id === 1
      }

    }

  }

}
