package com.github.mauricio.async.db.mysql.message.client

data class QueryMessage(val query: String) : ClientMessage(ClientMessage.Query)
