package com.github.jasync.sql.db.mysql

import com.github.jasync.sql.db.util.ExecutorServiceUtils
import com.github.jasync.sql.db.util.flatMap
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TransactionTest : ConnectionHelper() {

    @Test
    fun transaction_should_complete() {
        withConnection { conn ->
            executeQuery(conn, this.createTable)
            val future = conn.inTransaction(ExecutorServiceUtils.CachedThreadPool) {
                c -> c.sendQuery(this.insert).flatMap(ExecutorServiceUtils.CachedThreadPool) { _ -> conn.sendQuery(this.insert) }
            }
            val r = awaitFuture(future)
            assertEquals(1, r.rowsAffected)
            val result = assertNotNull(executePreparedStatement(conn, this.select))
            assertNotNull(result)
            assertEquals(2, result.rowsAffected)
            assertEquals("Boogie Man", result.rows?.get(0)?.get("name"))
            assertEquals("Boogie Man", result.rows?.get(1)?.get("name"))
        }

    }

    @Test
    @Ignore
    fun transaction_should_complete_prepared() {
        withConnection { conn ->
            executeQuery(conn, this.createTable)
            val future = conn.inTransaction(ExecutorServiceUtils.CachedThreadPool) {
                c -> c.sendPreparedStatement(this.insert).flatMap(ExecutorServiceUtils.CachedThreadPool) { _ -> conn.sendPreparedStatement(this.insert) }
            }
            awaitFuture(future)
            val result = assertNotNull(executePreparedStatement(conn, this.select))
            assertNotNull(result)
            assertEquals(2, result.rowsAffected)
            assertEquals("Boogie Man", result.rows?.get(0)?.get("name"))
            assertEquals("Boogie Man", result.rows?.get(1)?.get("name"))
        }

    }

}

