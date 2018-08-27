package com.github.jasync.sql.db.mysql.message.client

data class QueryMessage(val query: String) : ClientMessage(ClientMessage.Query)
