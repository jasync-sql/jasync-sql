package com.github.jasync.sql.db.mysql

import org.junit.Test

class QueryMoreSpec : ConnectionHelper() {

    @Test
    fun `"connection" should   "be able to select with empty value" `() {
        withConnection { connection ->
            executeQuery(connection, "select '' as x,'1' as y ")
        }
    }
}
