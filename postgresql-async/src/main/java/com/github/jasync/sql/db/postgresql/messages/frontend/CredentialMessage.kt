package com.github.jasync.sql.db.postgresql.messages.frontend

import com.github.jasync.sql.db.postgresql.messages.backend.AuthenticationResponseType
import com.github.jasync.sql.db.postgresql.messages.backend.ServerMessage


class CredentialMessage(val username: String,
                        val password: String,
                        val authenticationType: AuthenticationResponseType,
                        val salt: ByteArray?) : ClientMessage(ServerMessage.PasswordMessage)
