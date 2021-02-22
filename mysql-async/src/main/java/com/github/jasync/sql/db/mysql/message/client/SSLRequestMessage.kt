package com.github.jasync.sql.db.mysql.message.client

import com.github.jasync.sql.db.mysql.util.CapabilityFlag

data class SSLRequestMessage(val flags: Set<CapabilityFlag>) : ClientMessage(SslRequest)
