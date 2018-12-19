package com.github.jasync.sql.db.mysql.pool

import com.github.jasync.sql.db.exceptions.ConnectionNotConnectedException
import com.github.jasync.sql.db.mysql.ConnectionHelper
import com.github.jasync.sql.db.util.isFailure
import com.github.jasync.sql.db.util.isSuccess
import org.junit.Test
import java.util.concurrent.ExecutionException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MySQLConnectionFactorySpec : ConnectionHelper() {
    val factory = MySQLConnectionFactory(getConfiguration())

    @Test
    fun `fail validation if a connection has errored`() {
        val connection = factory.create().get()

        try {
            executeQuery(connection, "this is not sql")
        } catch (ignore: Exception) {
        }

        try {
            if (factory.validate(connection).isSuccess) {
                throw IllegalStateException("should not have come here")
            }
        } finally {
            awaitFuture(connection.close())
        }

    }

    @Test
    fun `it should take a connection from the pool and the pool should not accept it back if it is broken`() {
        withPool { pool ->
            val connection = awaitFuture(pool.take())

            assertEquals(1, pool.inUse().size)

            awaitFuture(connection.disconnect())

            try {
                awaitFuture(pool.giveBack(connection))
            } catch (e: ExecutionException) {
                assertNotNull(e.cause)
                assertTrue(e.cause is ConnectionNotConnectedException)
            }
            assertEquals(0, pool.inUse().size)
        }
    }

    @Test
    fun `be able to provide connections to the pool`() {
        withPool { pool ->
            assertEquals(0L, assertNotNull(executeQuery(pool, "SELECT 0").rows)[0].get(0))
        }
    }

    @Test
    fun `fail validation if a connection is disconnected`() {
        val connection = factory.create().get()

        awaitFuture(connection.disconnect())

        assertTrue(factory.validate(connection).isFailure)
    }

    @Test
    fun `fail validation if a connection is still waiting for a query`() {
        val connection = factory.create().get()
        connection.sendQuery("SELECT SLEEP(10)")

        Thread.sleep(1000)
        assertFalse(factory.validate(connection).isSuccess)
        assertEquals(connection, awaitFuture(connection.close()))
    }

    fun `accept a good connection`() {
        val connection = factory.create().get()
        assertFalse(factory.validate(connection).isFailure)
        assertEquals(connection, awaitFuture(connection.close()))
    }

    fun `test a valid connection and say it is ok`() {

        val connection = factory.create().get()

        assertTrue(factory.test(connection).isSuccess)
        assertEquals(connection, awaitFuture(connection.close()))

    }

    fun `fail test if a connection is disconnected`() {
        val connection = factory.create().get()

        awaitFuture(connection.disconnect())
        assertTrue(factory.test(connection).isFailure)
    }
}
