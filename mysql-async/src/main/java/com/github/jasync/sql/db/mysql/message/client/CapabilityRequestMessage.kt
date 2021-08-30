package com.github.jasync.sql.db.mysql.message.client

import com.github.jasync.sql.db.mysql.util.CapabilityFlag

data class CapabilityRequestMessage(val flags: Set<CapabilityFlag>) : ClientMessage(SslRequest)
