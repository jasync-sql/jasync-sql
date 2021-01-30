package com.github.jasync.sql.db.mysql.message.client

data class SSLRequestMessage(val connectWithDb: Boolean, val hasAppName: Boolean) : ClientMessage(SslRequest)
