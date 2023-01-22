package com.github.jasync.sql.db.mysql.message.client

import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.mysql.message.server.AuthenticationSwitchRequest

data class AuthenticationSwitchResponse(
    val configuration: Configuration,
    val request: AuthenticationSwitchRequest,
) : ClientMessage(AuthSwitchResponse)
