package com.github.aysnc.sql.db.integration.pool

import com.github.aysnc.sql.db.integration.DatabaseTestHelper
import com.github.aysnc.sql.db.verifyException
import com.github.jasync.sql.db.invoke
import com.github.jasync.sql.db.pool.ConnectionPool
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import com.github.jasync.sql.db.postgresql.exceptions.GenericDatabaseException
import com.github.jasync.sql.db.util.ExecutorServiceUtils
import com.github.jasync.sql.db.util.flatMapAsync
import com.github.jasync.sql.db.util.mapAsync
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.*
import java.util.concurrent.ExecutionException


class ConnectionPoolSpec : DatabaseTestHelper() {
    private val Insert = "insert into transaction_test (id) values (?)"


    @Test
    fun `"pool" should "give you a connection when sending statements"`() {

        withPool { pool ->
            assertThat(executeQuery(pool, "SELECT 8").rows.get(0)(0)).isEqualTo(8)
            Thread.sleep(1000)
            assertThat(pool.idleConnectionsCount).isEqualTo(1)
        }

    }

    @Test
    fun `"pool" should "give you a connection for prepared statements"`() {
        withPool { pool ->
            assertThat(executePreparedStatement(pool, "SELECT 8").rows.get(0)(0)).isEqualTo(8)
            Thread.sleep(1000)
            assertThat(pool.idleConnectionsCount).isEqualTo(1)
        }
    }

    @Test
    fun `"pool" should "return an empty map when connect is called"`() {
        withPool { pool ->
            assertThat(awaitFuture(pool.connect())).isEqualTo(pool)
        }
    }

    @Test
    fun `"pool" should "runs commands for a transaction in a single connection" `() {

        val id = UUID.randomUUID().toString()

        withPool { pool ->
            val operations = pool.inTransaction { connection ->
                connection.sendPreparedStatement(Insert, listOf(id))
                    .flatMapAsync(ExecutorServiceUtils.CommonPool) { result ->
                        connection.sendPreparedStatement(Insert, listOf(id))
                            .mapAsync(ExecutorServiceUtils.CommonPool) { failure ->
                                listOf(result, failure)
                            }
                    }
            }
            verifyException(ExecutionException::class.java, GenericDatabaseException::class.java) {
                awaitFuture(operations)
            }

        }

    }


    private fun <R> withPool(fn: (ConnectionPool<PostgreSQLConnection>) -> R): R {
TODO()
//        val pool = ConnectionPool(PostgreSQLConnectionFactory(defaultConfiguration), ConnectionPoolConfigurationBuilder() PoolConfiguration(10, 4, 10))
//        try {
//            return fn(pool)
//        } finally {
//            pool.disconnect().get()
//        }

    }


}
