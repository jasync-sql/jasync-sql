package com.github.aysnc.sql.db.integration

import com.github.jasync.sql.db.invoke
import com.github.jasync.sql.db.postgresql.exceptions.GenericDatabaseException
import com.github.jasync.sql.db.util.ExecutorServiceUtils
import com.github.jasync.sql.db.util.flatMap
import com.github.jasync.sql.db.util.length
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.concurrent.ExecutionException


class TransactionSpec : DatabaseTestHelper() {


  val tableCreate = "CREATE TEMP TABLE transaction_test (x integer PRIMARY KEY)"

  fun tableInsert(x: Int) = "INSERT INTO transaction_test VALUES (" + x.toString() + ")"

  val tableSelect = "SELECT x FROM transaction_test ORDER BY x"

  @Test
  fun `"transactions" should "commit simple inserts"`() {
    withHandler { handler ->
      executeDdl(handler, tableCreate)
      await(handler.inTransaction { conn ->
        conn.sendQuery(tableInsert(1)).flatMap(ExecutorServiceUtils.CommonPool) { _ ->
          conn.sendQuery(tableInsert(2))
        }
      })

      val rows = executeQuery(handler, tableSelect).rows!!
      assertThat(rows.length).isEqualTo(2)
      assertThat(rows(0)(0)).isEqualTo(1)
      assertThat(rows(1)(0)).isEqualTo(2)
    }
  }

  @Test
  fun `"transactions" should "commit simple inserts , prepared statements"`() {
    withHandler { handler ->
      executeDdl(handler, tableCreate)
      await(handler.inTransaction { conn ->
        conn.sendPreparedStatement(tableInsert(1)).flatMap(ExecutorServiceUtils.CommonPool) { _ ->
          conn.sendPreparedStatement(tableInsert(2))
        }
      })

      val rows = executePreparedStatement(handler, tableSelect)!!.rows!!
      assertThat(rows.length).isEqualTo(2)
      assertThat(rows(0)(0)).isEqualTo(1)
      assertThat(rows(1)(0)).isEqualTo(2)
    }
  }

  @Test
  fun `"transactions" should "rollback on error"`() {
    withHandler { handler ->
      executeDdl(handler, tableCreate)

      val e: GenericDatabaseException = verifyException(ExecutionException::class.java, GenericDatabaseException::class.java) {
        await(handler.inTransaction { conn ->
          conn.sendQuery(tableInsert(1)).flatMap(ExecutorServiceUtils.CommonPool) { _ ->
            conn.sendQuery(tableInsert(1))
          }
        })
      } as GenericDatabaseException

      assertThat(e.errorMessage.message).isEqualTo("duplicate key value violates unique constraint \"transaction_test_pkey\"")


      val rows = executeQuery(handler, tableSelect).rows!!
      assertThat(rows.length).isEqualTo(0)
    }

  }

  @Test
  fun `"transactions" should "rollback explicitly"`() {
    withHandler { handler ->
      executeDdl(handler, tableCreate)
      await(handler.inTransaction { conn ->
        conn.sendQuery(tableInsert(1)).flatMap(ExecutorServiceUtils.CommonPool) { _ ->
          conn.sendQuery("ROLLBACK")
        }
      })

      val rows = executeQuery(handler, tableSelect).rows!!
      assertThat(rows.length).isEqualTo(0)
    }

  }

  @Test
  fun `"transactions" should "rollback to savepoint"`() {
    withHandler { handler ->
      executeDdl(handler, tableCreate)
      await(handler.inTransaction { conn ->
        conn.sendQuery(tableInsert(1)).flatMap(ExecutorServiceUtils.CommonPool) { _ ->
          conn.sendQuery("SAVEPOINT one").flatMap(ExecutorServiceUtils.CommonPool) { _ ->
            conn.sendQuery(tableInsert(2)).flatMap(ExecutorServiceUtils.CommonPool) { _ ->
              conn.sendQuery("ROLLBACK TO SAVEPOINT one")
            }
          }
        }
      })

      val rows = executeQuery(handler, tableSelect).rows!!
      assertThat(rows.length).isEqualTo(1)
      assertThat(rows[0](0)).isEqualTo(1)
    }

  }


  private fun verifyException(exType: Class<out java.lang.Exception>,
                              causeType: Class<out java.lang.Exception>? = null,
                              body: () -> Unit): Throwable {
    try {
      body()
      throw Exception("${exType.simpleName}->${causeType?.simpleName} was not thrown")
    } catch (e: Exception) {
      //e.printStackTrace()
      assertThat(e::class.java).isEqualTo(exType)
      causeType?.let { assertThat(e.cause!!::class.java).isEqualTo(it) }
      return e.cause ?: e
    }
  }

}
