package com.github.aysnc.sql.db.integration

import com.github.jasync.sql.db.invoke
import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
import org.junit.Test

class NumericSpec : DatabaseTestHelper() {

    @Test
    fun `"when processing numeric columns" should "support first update of num column with floating" `() {

        withHandler { handler ->
            executeDdl(handler, "CREATE TEMP TABLE numeric_test (id BIGSERIAL, numcol NUMERIC)")

            val id = executePreparedStatement(
                handler,
                "INSERT INTO numeric_test DEFAULT VALUES RETURNING id"
            ).rows.get(0)("id")
            executePreparedStatement(handler, "UPDATE numeric_test SET numcol = ? WHERE id = ?", listOf(123.123, id))
            executePreparedStatement(handler, "UPDATE numeric_test SET numcol = ? WHERE id = ?", listOf(1234, id))
            executePreparedStatement(handler, "UPDATE numeric_test SET numcol = ? WHERE id = ?", listOf(123.123, id))

            assertThat(id).isEqualTo(1L)
        }
    }

    @Ignore("this test fail since always, see https://github.com/jasync-sql/jasync-sql/issues/15 and https://github.com/mauricio/postgresql-async/blob/b96aaf163e6ce757e722e95763a9dbc6f90211d5/postgresql-async/src/test/scala/com/github/mauricio/async/db/postgresql/NumericSpec.scala")
    @Test
    fun `"when processing numeric columns" should "support first update of num column with integer" `() {

        withHandler { handler ->
            executeDdl(handler, "CREATE TEMP TABLE numeric_test (id BIGSERIAL, numcol NUMERIC)")

            val id = executePreparedStatement(
                handler,
                "INSERT INTO numeric_test DEFAULT VALUES RETURNING id"
            ).rows.get(0)("id")
            executePreparedStatement(handler, "UPDATE numeric_test SET numcol = ? WHERE id = ?", listOf(1234, id))
            executePreparedStatement(handler, "UPDATE numeric_test SET numcol = ? WHERE id = ?", listOf(123.123, id))

            assertThat(id).isEqualTo(1L)
        }
    }

    @Test
    fun `"when processing numeric columns" should "support using first update with queries instead of prepared statements" `() {

        withHandler { handler ->
            executeDdl(handler, "CREATE TEMP TABLE numeric_test (id BIGSERIAL, numcol NUMERIC)")

            val id = executeQuery(handler, "INSERT INTO numeric_test DEFAULT VALUES RETURNING id").rows.get(0)("id")
            executeQuery(handler, "UPDATE numeric_test SET numcol = 1234 WHERE id = $id")
            executeQuery(handler, "UPDATE numeric_test SET numcol = 123.123 WHERE id = $id")

            assertThat(id).isEqualTo(1L)
        }
    }
}
