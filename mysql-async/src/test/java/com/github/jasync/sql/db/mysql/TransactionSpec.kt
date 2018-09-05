package com.github.jasync.sql.db.mysql

//import com.github.jasync.sql.db.util.ExecutorServiceUtils
//import com.github.jasync.sql.db.util.flatMap
//import org.junit.Test
//import kotlin.test.assertEquals
//import kotlin.test.assertNotNull

class TransactionTest : ConnectionHelper() {

//    @Test
//    fun transaction_should_complete() {
//        withConnection { conn ->
//            executeQuery(conn, this.createTable)
//            awaitFuture(conn.inTransaction(ExecutorServiceUtils.CachedThreadPool) {
//                conn.sendPreparedStatement(this.insert).flatMap(ExecutorServiceUtils.CachedThreadPool) { _ -> conn.sendPreparedStatement(this.insert) }
//            })
//            val result = assertNotNull(executeQuery(conn, this.select))
//            assertNotNull(result)
//            assertEquals(2, result.rowsAffected)
//            assertEquals("Boogie Man", result.rows?.get(0)?.get("name"))
//            assertEquals("Boogie Man", result.rows?.get(1)?.get("name"))
//        }
//    }
}
