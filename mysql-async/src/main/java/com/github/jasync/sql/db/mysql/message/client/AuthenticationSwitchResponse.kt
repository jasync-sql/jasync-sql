package com.github.jasync.sql.db.mysql.message.client

import com.github.jasync.sql.db.SSLConfiguration
import com.github.jasync.sql.db.mysql.message.server.AuthenticationSwitchRequest
import java.nio.file.Path

data class AuthenticationSwitchResponse(
    val password: String?,
    val sslConfiguration: SSLConfiguration,
    val rsaPublicKey: Path?,
    val request: AuthenticationSwitchRequest,
) : ClientMessage(AuthSwitchResponse)
