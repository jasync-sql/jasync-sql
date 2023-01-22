package com.github.jasync.sql.db.mysql.message.client

import com.github.jasync.sql.db.Configuration
import java.nio.charset.Charset

data class HandshakeResponseMessage(
    val header: CapabilityRequestMessage,
    val username: String,
    val charset: Charset,
    @Suppress("ArrayInDataClass")
    val seed: ByteArray,
    val authenticationMethod: String,
    val password: String? = null,
    val database: String? = null,
    val appName: String? = null,
    val configuration: Configuration,
) : ClientMessage(ClientProtocolVersion)
