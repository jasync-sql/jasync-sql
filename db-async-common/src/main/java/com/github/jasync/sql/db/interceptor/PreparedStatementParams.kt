package com.github.jasync.sql.db.interceptor

data class PreparedStatementParams(
    val query: String,
    val values: List<Any?>,
    val release: Boolean
)
