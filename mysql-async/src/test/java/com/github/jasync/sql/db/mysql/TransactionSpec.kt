package com.github.jasync.sql.db.mysql

import com.github.jasync.sql.db.invoke
import com.github.jasync.sql.db.mysql.exceptions.MySQLException
import com.github.jasync.sql.db.util.ExecutorServiceUtils
import com.github.jasync.sql.db.util.flatMapAsync
import com.github.jasync.sql.db.util.mapAsync
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.UUID
import java.util.concurrent.ExecutionException

val BrokenInsert = """INSERT INTO users (id, name) VALUES (1, 'Maurício Aragão')"""
val TransactionInsert = "insert into transaction_test (id) values (?)"

class TransactionSpec : ConnectionHelper() {

    @Test
    fun `connection in transaction should correctly store the values of the transaction`() {
        withConnection { connection ->
            executeQuery(connection, this.createTable)

            val future = connection.inTransaction { c ->
                c.sendPreparedStatement(this.insert)
                    .flatMapAsync(ExecutorServiceUtils.CommonPool) { connection.sendPreparedStatement(this.insert) }
            }

            future.get()

            val result = executePreparedStatement(connection, this.select).rows
            assertThat(result.size).isEqualTo(2)

            assertThat(result[0]["name"]).isEqualTo("Boogie Man")
            assertThat(result[1]("name")).isEqualTo("Boogie Man")
        }
    }

    private fun verifyException(
        exType: Class<out java.lang.Exception>,
        causeType: Class<out java.lang.Exception>? = null,
        body: () -> Unit
    ): Throwable {
        try {
            body()
            throw Exception("${exType.simpleName}->${causeType?.simpleName} was not thrown")
        } catch (e: Exception) {
            // e.printStackTrace()
            assertThat(e::class.java).isEqualTo(exType)
            causeType?.let { assertThat(e.cause!!::class.java).isEqualTo(it) }
            return e.cause ?: e
        }
    }

    @Test
    fun `connection in transaction should correctly rollback changes if the transaction raises an exception`() {
        withConnection { connection ->
            executeQuery(connection, this.createTable)
            executeQuery(connection, this.insert)

            val future = connection.inTransaction { c ->
                c.sendQuery(this.insert)
                    .flatMapAsync(ExecutorServiceUtils.CommonPool) { c.sendQuery(BrokenInsert) }
            }

            val e: MySQLException = verifyException(ExecutionException::class.java, MySQLException::class.java) {
                awaitFuture(future)
            } as MySQLException

            assertThat(e.errorMessage.errorCode).isEqualTo(1062)
            assertThat(e.errorMessage.errorMessage).matches("Duplicate entry '1' for key '(users\\.)?PRIMARY'")

            val result = executePreparedStatement(connection, this.select).rows
            assertThat(result.size).isEqualTo(1)
            assertThat(result[0]("name")).isEqualTo("Boogie Man")
        }
    }

    @Test
    fun `connection in transaction should should make a connection invalid and not return it to the pool if it raises an exception`() {
        withPool { pool ->

            executeQuery(pool, this.createTable)
            executeQuery(pool, this.insert)

            val future = pool.inTransaction { c ->
                c.sendQuery(BrokenInsert)
            }

            verifyException(ExecutionException::class.java, MySQLException::class.java) {
                awaitFuture(future)
            } as MySQLException

            assertThat(pool.idleConnectionsCount).isEqualTo(0)
            // pool.availables must not contain (connection.asInstanceOf[MySQLConnection])
        }
    }

    @Test
    fun `connection in transaction should runs commands for a transaction in a single connection`() {
        val id = UUID.randomUUID().toString()

        withPool { pool ->
            val operations = pool.inTransaction { connection ->
                connection.sendPreparedStatement(TransactionInsert, listOf(id))
                    .flatMapAsync((ExecutorServiceUtils.CommonPool)) { result ->
                        connection.sendPreparedStatement(TransactionInsert, listOf(id))
                            .mapAsync(ExecutorServiceUtils.CommonPool) { failure ->
                                listOf(result, failure)
                            }
                    }
            }

            val e: MySQLException = verifyException(ExecutionException::class.java, MySQLException::class.java) {
                operations.get()
            } as MySQLException

            assertThat(e.errorMessage.errorCode).isEqualTo(1062)
            val rows = executePreparedStatement(pool, "select * from transaction_test where id = ?", listOf(id)).rows
            assertThat(rows).isEmpty()
        }
    }

    @Test
    fun `check auto-commit and in transaction flag`() {
        withConnection { connection ->
            assertThat(connection.isAutoCommit()).isTrue()
            awaitFuture(connection.sendQuery("SET AUTOCOMMIT=0"))
            assertThat(connection.isAutoCommit()).isFalse()
        }
    }
}
