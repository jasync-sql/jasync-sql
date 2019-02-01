package com.github.jasync.sql.db

interface Listener {
    fun onQuery(query: String)
    fun onQueryComplete(result: QueryResult)
    fun onQueryError(query: String)
}
