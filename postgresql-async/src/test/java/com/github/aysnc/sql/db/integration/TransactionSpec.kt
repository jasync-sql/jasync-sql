package com.github.aysnc.sql.db.integration

import com.github.aysnc.sql.db.verifyException
import com.github.jasync.sql.db.invoke
import com.github.jasync.sql.db.postgresql.exceptions.GenericDatabaseException
import com.github.jasync.sql.db.util.ExecutorServiceUtils
import com.github.jasync.sql.db.util.flatMapAsync
import com.github.jasync.sql.db.util.length
import java.util.concurrent.ExecutionException
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TransactionSpec : DatabaseTestHelper() {

    private val tableCreate = "CREATE TEMP TABLE transaction_test (x integer PRIMARY KEY)"

    private fun tableInsert(x: Int) = "INSERT INTO transaction_test VALUES ($x)"

    private val tableSelect = "SELECT x FROM transaction_test ORDER BY x"

    @Test
    fun `transactions should commit simple inserts`() {
        withHandler { handler ->
            executeDdl(handler, tableCreate)
            awaitFuture(handler.inTransaction { conn ->
                conn.sendQuery(tableInsert(1)).flatMapAsync(ExecutorServiceUtils.CommonPool) {
                    conn.sendQuery(tableInsert(2))
                }
            })

            val rows = executeQuery(handler, tableSelect).rows
            assertThat(rows.length).isEqualTo(2)
            assertThat(rows(0)(0)).isEqualTo(1)
            assertThat(rows(1)(0)).isEqualTo(2)
        }
    }

    @Test
    fun `transactions should commit simple inserts, prepared statements`() {
        withHandler { handler ->
            executeDdl(handler, tableCreate)
            awaitFuture(handler.inTransaction { conn ->
                conn.sendPreparedStatement(tableInsert(1)).flatMapAsync(ExecutorServiceUtils.CommonPool) {
                    conn.sendPreparedStatement(tableInsert(2))
                }
            })

            val rows = executePreparedStatement(handler, tableSelect).rows
            assertThat(rows.length).isEqualTo(2)
            assertThat(rows(0)(0)).isEqualTo(1)
            assertThat(rows(1)(0)).isEqualTo(2)
        }
    }

    @Test
    fun `transactions should rollback on error`() {
        withHandler { handler ->
            executeDdl(handler, tableCreate)

            val e: GenericDatabaseException =
                verifyException(ExecutionException::class.java, GenericDatabaseException::class.java) {
                    awaitFuture(handler.inTransaction { conn ->
                        conn.sendQuery(tableInsert(1)).flatMapAsync(ExecutorServiceUtils.CommonPool) {
                            conn.sendQuery(tableInsert(1))
                        }
                    })
                } as GenericDatabaseException

            assertThat(e.errorMessage.message).isEqualTo("duplicate key value violates unique constraint \"transaction_test_pkey\"")

            val rows = executeQuery(handler, tableSelect).rows
            assertThat(rows.length).isEqualTo(0)
        }
    }

    @Test
    fun `transactions should rollback explicitly`() {
        withHandler { handler ->
            executeDdl(handler, tableCreate)
            awaitFuture(handler.inTransaction { conn ->
                conn.sendQuery(tableInsert(1)).flatMapAsync(ExecutorServiceUtils.CommonPool) {
                    conn.sendQuery("ROLLBACK")
                }
            })

            val rows = executeQuery(handler, tableSelect).rows
            assertThat(rows.length).isEqualTo(0)
        }
    }

    @Test
    fun `transactions should rollback to savepoint`() {
        withHandler { handler ->
            executeDdl(handler, tableCreate)
            awaitFuture(handler.inTransaction { conn ->
                conn.sendQuery(tableInsert(1)).flatMapAsync(ExecutorServiceUtils.CommonPool) {
                    conn.sendQuery("SAVEPOINT one").flatMapAsync(ExecutorServiceUtils.CommonPool) {
                        conn.sendQuery(tableInsert(2)).flatMapAsync(ExecutorServiceUtils.CommonPool) {
                            conn.sendQuery("ROLLBACK TO SAVEPOINT one")
                        }
                    }
                }
            })

            val rows = executeQuery(handler, tableSelect).rows
            assertThat(rows.length).isEqualTo(1)
            assertThat(rows[0](0)).isEqualTo(1)
        }
    }
}
