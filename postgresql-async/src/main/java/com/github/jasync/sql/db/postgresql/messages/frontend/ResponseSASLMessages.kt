package com.github.jasync.sql.db.postgresql.messages.frontend

import com.github.jasync.sql.db.postgresql.messages.backend.ServerMessage

class SASLInitialResponse(val mechanism: String, val saslData: String) : ClientMessage(ServerMessage.PasswordMessage)
class SASLResponse(val saslData: String) : ClientMessage(ServerMessage.PasswordMessage)
