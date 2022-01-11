package com.github.jasync.sql.db.postgresql.util

@Suppress("RedundantUnitReturnType")
interface ArrayStreamingParserDelegate {

    fun arrayStarted() {}

    fun arrayEnded() {}

    fun elementFound(element: String) {}

    fun nullElementFound() {}
}
