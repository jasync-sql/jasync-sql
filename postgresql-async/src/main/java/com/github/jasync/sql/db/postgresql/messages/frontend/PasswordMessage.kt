package com.github.jasync.sql.db.postgresql.messages.frontend

import com.github.jasync.sql.db.postgresql.messages.backend.ServerMessage

class PasswordMessage(
    val username: String,
    val password: String,
    val salt: ByteArray?
) : ClientMessage(ServerMessage.PasswordMessage)
