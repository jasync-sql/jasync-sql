package com.github.jasync.sql.db

interface QueryListener {
    fun onQuery(query: String)
    fun onQueryComplete(result: QueryResult)
    fun onQueryError(query: Throwable)

    fun onPreparedStatement(query: String, values: List<Any?>)
    fun onPreparedStatementComplete(result: QueryResult)
    fun onPreparedStatementError(query: Throwable)
}
