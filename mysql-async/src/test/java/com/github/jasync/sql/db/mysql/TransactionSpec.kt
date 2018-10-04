package com.github.jasync.sql.db.mysql

import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.invoke
import com.github.jasync.sql.db.mysql.exceptions.MySQLException
import com.github.jasync.sql.db.util.ExecutorServiceUtils
import com.github.jasync.sql.db.util.flatMap
import com.github.jasync.sql.db.util.map
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.UUID
import java.util.concurrent.ExecutionException

val BrokenInsert = """INSERT INTO users (id, name) VALUES (1, 'Maurício Aragão')"""
val InsertUser = """INSERT INTO users (name) VALUES (?)"""
val TransactionInsert = "insert into transaction_test (id) values (?)"

class TransactionSpec : ConnectionHelper() {

  @Test
  fun `"connection in transaction" should "correctly store the values of the transaction"`() {
    withConnection { connection ->
      val r1 = executeQuery(connection, this.createTable)

      val future = connection.inTransaction { c ->
        c.sendPreparedStatement(this.insert)
            .flatMap(ExecutorServiceUtils.CommonPool) { r -> connection.sendPreparedStatement(this.insert) }
      }

      val r2 = future.get()

      val result = executePreparedStatement(connection, this.select).rows!!
      assertThat(result.size).isEqualTo(2)

      assertThat(result[0]["name"]).isEqualTo("Boogie Man")
      assertThat(result[1]("name")).isEqualTo("Boogie Man")
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

  @Test
  fun `"connection in transaction" should "correctly rollback changes if the transaction raises an exception"`() {

    withConnection { connection ->
      executeQuery(connection, this.createTable)
      executeQuery(connection, this.insert)

      val future = connection.inTransaction { c ->
        c.sendQuery(this.insert).flatMap(ExecutorServiceUtils.CommonPool) { r -> c.sendQuery(BrokenInsert) }
      }

      val e: MySQLException = verifyException(ExecutionException::class.java, MySQLException::class.java) {
        awaitFuture(future)
      } as MySQLException

      assertThat(e.errorMessage.errorCode).isEqualTo(1062)
      assertThat(e.errorMessage.errorMessage).isEqualTo("Duplicate entry '1' for key 'PRIMARY'")

      val result = executePreparedStatement(connection, this.select).rows!!
      assertThat(result.size).isEqualTo(1)
      assertThat(result[0]("name")).isEqualTo("Boogie Man")
    }

  }

  @Test
  fun `"connection in transaction" should "should make a connection invalid and not return it to the pool if it raises an exception"`() {


    withPool { pool ->

      executeQuery(pool, this.createTable)
      executeQuery(pool, this.insert)

      var connection: Connection? = null

      val future = pool.inTransaction { c ->
        connection = c
        c.sendQuery(BrokenInsert)
      }

      verifyException(ExecutionException::class.java, MySQLException::class.java) {
        awaitFuture(future)
      } as MySQLException

      assertThat(pool.availables()).isEmpty()
      //pool.availables must not contain (connection.asInstanceOf[MySQLConnection])

    }

  }

  @Test
  fun `"connection in transaction" should "runs commands for a transaction in a single connection"`() {

    val id = UUID.randomUUID().toString()

    withPool { pool ->
      val operations = pool.inTransaction { connection ->
        connection.sendPreparedStatement(TransactionInsert, listOf(id)).flatMap((ExecutorServiceUtils.CommonPool)) { result ->
          connection.sendPreparedStatement(TransactionInsert, listOf(id)).map(ExecutorServiceUtils.CommonPool) { failure ->
            listOf(result, failure)
          }
        }
      }

      val e : MySQLException = verifyException(ExecutionException::class.java, MySQLException::class.java) {
        operations.get()
      } as MySQLException

      assertThat(e.errorMessage.errorCode).isEqualTo(1062)
      val rows = executePreparedStatement(pool, "select * from transaction_test where id = ?", listOf(id)).rows
      assertThat(rows).isEmpty()

    }

  }


}
