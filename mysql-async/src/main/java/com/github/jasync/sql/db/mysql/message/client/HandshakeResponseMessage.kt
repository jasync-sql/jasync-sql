package com.github.jasync.sql.db.mysql.message.client

import java.nio.charset.Charset

data class HandshakeResponseMessage(
    val username: String,
    val charset: Charset,
    val seed: ByteArray,
    val authenticationMethod: String,
    val password: String? = null,
    val database: String? = null
) : ClientMessage(ClientMessage.ClientProtocolVersion)
