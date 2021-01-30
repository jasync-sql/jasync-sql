package com.github.jasync.sql.db.mysql.message.client

import java.nio.charset.Charset

data class HandshakeResponseMessage(
    val header: SSLRequestMessage,
    val username: String,
    val charset: Charset,
    @Suppress("ArrayInDataClass")
    val seed: ByteArray,
    val authenticationMethod: String,
    val password: String? = null,
    val database: String? = null,
    val appName: String? = null
) : ClientMessage(ClientProtocolVersion)
