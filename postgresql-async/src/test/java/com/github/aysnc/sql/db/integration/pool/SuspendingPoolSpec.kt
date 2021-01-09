package com.github.aysnc.sql.db.integration.pool

import com.github.aysnc.sql.db.integration.DatabaseTestHelper
import com.github.aysnc.sql.db.verifyException
import com.github.jasync.sql.db.asSuspending
import com.github.jasync.sql.db.invoke
import com.github.jasync.sql.db.pool.PoolAlreadyTerminatedException
import com.github.jasync.sql.db.postgresql.exceptions.GenericDatabaseException
import com.github.jasync.sql.db.util.length
import java.util.concurrent.ThreadLocalRandom
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert
import org.junit.Test

class SuspendingPoolSpec : DatabaseTestHelper() {

    private val tableCreate = "CREATE TEMP TABLE transaction_test (x integer PRIMARY KEY)"

    private fun tableInsert(x: Int) = "INSERT INTO transaction_test VALUES ($x)"

    private val tableSelect = "SELECT x FROM transaction_test ORDER BY x"

    @Test
    fun `SuspendingConnection pool 'connect' should not do anything`() {
        withPool { pool ->
            runBlocking {
                val tested = pool.asSuspending
                val connection = tested.connect()
                assertThat(connection).isEqualTo(pool)
            }
        }
    }

    @Test
    fun `SuspendingConnection pool 'disconnect' should not allow more queries`() {
        withPool { pool ->
            runBlocking {
                val tested = pool.asSuspending
                val connection = tested.disconnect()
                try {
                    connection.sendQuery("select 1")
                    Assert.fail("should not allow queries")
                } catch (e: PoolAlreadyTerminatedException) {
                    println("exception caught as expected")
                    // it is ok
                }
            }
        }
    }

    @Test
    fun `SuspendingConnection pool simple send query`() {
        withPool { pool ->
            runBlocking {
                val tested = pool.asSuspending
                val result = tested.sendQuery("select 3")
                assertThat(result.rows[0][0]).isEqualTo(3)
            }
        }
    }

    @Test
    fun `SuspendingConnection pool simple send prepared statement`() {
        withPool { pool ->
            runBlocking {
                val tested = pool.asSuspending
                val result1 = tested.sendPreparedStatement("select 3")
                assertThat(result1.rows[0][0]).isEqualTo(3)
                val result2 = tested.sendPreparedStatement("select 3", listOf())
                assertThat(result2.rows[0][0]).isEqualTo(3)
                val result3 = tested.sendPreparedStatement("select 3", listOf(), true)
                assertThat(result3.rows[0][0]).isEqualTo(3)
            }
        }
    }

    @Test
    fun `transactions should commit simple inserts , prepared statements`() {
        withHandler { connection ->
            val tested = connection.asSuspending
            runBlocking {
                tested.sendQuery(tableCreate)
                tested.inTransaction { suspConnection ->
                    suspConnection.sendPreparedStatement(tableInsert(1))
                    delay(500)
                    suspConnection.sendPreparedStatement(tableInsert(2))
                }

                val rows = tested.sendPreparedStatement(tableSelect).rows
                assertThat(rows.length).isEqualTo(2)
                assertThat(rows(0)(0)).isEqualTo(1)
                assertThat(rows(1)(0)).isEqualTo(2)
            }
        }
    }

    @Test
    fun `transactions should rollback on error`() {
        withHandler { connection ->
            val tested = connection.asSuspending
            runBlocking {
                tested.sendQuery(tableCreate)
                tested.inTransaction { suspConnection ->
                    suspConnection.sendPreparedStatement(tableInsert(1))
                    val e: GenericDatabaseException =
                            verifyException(GenericDatabaseException::class.java) {
                                runBlocking {
                                    suspConnection.sendPreparedStatement(tableInsert(1))
                                }
                            } as GenericDatabaseException
                    assertThat(e.errorMessage.message).isEqualTo("duplicate key value violates unique constraint \"transaction_test_pkey\"")
                }
            }

            val rows = executeQuery(connection, tableSelect).rows
            assertThat(rows.length).isEqualTo(0)
        }
    }

    @Test
    fun `transactions with pool should commit simple inserts , prepared statements`() {
        withPool { pool ->
            val tested = pool.asSuspending
            runBlocking {
                tested.sendQuery(tableCreate)
                tested.inTransaction { suspConnection ->
                    suspConnection.sendPreparedStatement(tableInsert(1))
                    // this makes sure if connection is released that it will not be reused
                    pool.sendQuery("SLEEP 100")
                    delay(500)
                    suspConnection.sendPreparedStatement(tableInsert(2))
                }

                val rows = tested.sendPreparedStatement(tableSelect).rows
                assertThat(rows.length).isEqualTo(2)
                assertThat(rows(0)(0)).isEqualTo(1)
                assertThat(rows(1)(0)).isEqualTo(2)
            }
        }
    }

    @Test
    fun `transactions with pool should rollback on error`() {
        val uniqID = ThreadLocalRandom.current().nextInt(100000)
        val tableName = "transaction_test_$uniqID"
        withPool { connection ->
            val tested = connection.asSuspending
            runBlocking {
                tested.sendQuery("CREATE TABLE $tableName (x integer PRIMARY KEY)")
                tested.inTransaction { suspConnection ->
                    suspConnection.sendPreparedStatement("INSERT INTO $tableName VALUES (1)")
                    val e: GenericDatabaseException =
                            verifyException(GenericDatabaseException::class.java) {
                                runBlocking {
                                    suspConnection.sendPreparedStatement("INSERT INTO $tableName VALUES (1)")
                                }
                            } as GenericDatabaseException
                    assertThat(e.errorMessage.message).isEqualTo("duplicate key value violates unique constraint \"${tableName}_pkey\"")
                }
            }

            val rows = executeQuery(connection, "SELECT x FROM $tableName ORDER BY x").rows
            assertThat(rows.length).isEqualTo(0)
        }
    }
}
