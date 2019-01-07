package com.github.jasync.sql.db.postgresql.util

@Suppress("RedundantUnitReturnType")
interface ArrayStreamingParserDelegate {

    fun arrayStarted(): Unit {}

    fun arrayEnded(): Unit {}

    fun elementFound(element: String): Unit {}

    fun nullElementFound(): Unit {}

}
