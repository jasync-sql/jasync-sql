package com.github.jasync.sql.db.mysql.message.client

import com.github.jasync.sql.db.mysql.message.server.AuthenticationSwitchRequest

data class AuthenticationSwitchResponse(val password: String?, val request: AuthenticationSwitchRequest) :
    ClientMessage(AuthSwitchResponse)
