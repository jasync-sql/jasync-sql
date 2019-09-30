package com.github.jasync.sql.db.mysql

import com.github.jasync.sql.db.ResultSet
import com.github.jasync.sql.db.invoke
import com.github.jasync.sql.db.util.head
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test


class StoredProceduresSpec : ConnectionHelper() {

    @Test
    fun `be able to execute create stored procedure`() {
        withConnection { connection ->
            connection.sendQuery("DROP PROCEDURE IF exists helloWorld;").get()
            val result = connection.sendQuery(
                """
               CREATE PROCEDURE helloWorld(OUT param1 VARCHAR(20))
               BEGIN
                 SELECT 'hello' INTO param1;
               END
              """
            ).get()
            assertThat(result.statusMessage).isEqualTo("")
        }
    }

    @Test
    fun `be able to call stored procedure`() {
        withConnection { connection ->
            connection.sendQuery("DROP PROCEDURE IF exists constTest;").get()
            connection.sendQuery(
                """
               CREATE PROCEDURE constTest(OUT param INT)
               BEGIN
                 SELECT 125 INTO param;
               END
              """
            ).get()
            connection.sendQuery("CALL constTest(@arg)").get()
            val result: ResultSet = connection.sendQuery("SELECT @arg").get().rows
            assertThat(result.size).isEqualTo(1)
            assertThat(result(0)(result.columnNames().head)).isEqualTo(125L)
        }
    }

    @Test
    fun `be able to call stored procedure - reproduce ok message sent after EOF`() {
        withConnection { connection ->
            connection.sendQuery("DROP PROCEDURE IF exists constTest;").get()
            connection.sendQuery(
                """
               CREATE PROCEDURE constTest()
               BEGIN
                 SELECT '1' column1, '2' column2;
               END
              """
            ).get()
            repeat(100) {
                val sendQueryResult = connection.sendQuery("CALL constTest()").get()
                assertThat(sendQueryResult.rows.size).isEqualTo(1)
            }
        }
    }

    @Test
    fun `be able to call stored procedure with input parameter`() {
        withConnection { connection ->
            connection.sendQuery("DROP PROCEDURE IF exists addTest;").get()
            connection.sendQuery(
                """
               CREATE PROCEDURE addTest(IN a INT, IN b INT, OUT sum INT)
               BEGIN
                 SELECT a+b INTO sum;
               END
              """
            ).get()
            connection.sendQuery("CALL addTest(132, 245, @sm)").get()
            val result = connection.sendQuery("SELECT @sm").get()
            val rows = result.rows
            assertThat(rows.size).isEqualTo(1)
            assertThat(rows(0)(rows.columnNames().head)).isEqualTo(377L)
        }
    }

    @Test
    fun `be able to remove stored procedure`() {
        withConnection { connection ->
            connection.sendQuery("DROP PROCEDURE IF exists remTest;").get()
            connection.sendQuery(
                """
                  CREATE PROCEDURE remTest(OUT cnst INT)
                     BEGIN
                       SELECT 987 INTO cnst;
                     END
                """
            ).get()
            val rows = connection.sendQuery(
                """
                  SELECT routine_name FROM INFORMATION_SCHEMA.ROUTINES WHERE routine_name="remTest"
                """
            ).get().rows

            assertThat(rows.size).isEqualTo(1)
            assertThat(rows.get(0)("routine_name")).isEqualTo("remTest")

            connection.sendQuery("DROP PROCEDURE remTest;").get()
            val removeResult = connection.sendQuery(
                """
                  SELECT routine_name FROM INFORMATION_SCHEMA.ROUTINES WHERE routine_name="remTest"
                """
            ).get().rows
            assertThat(removeResult.isEmpty()).isEqualTo(true)
        }
    }
}


