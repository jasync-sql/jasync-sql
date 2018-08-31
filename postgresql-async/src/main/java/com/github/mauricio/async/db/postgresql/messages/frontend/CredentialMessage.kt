package com.github.mauricio.async.db.postgresql.messages.frontend

import com.github.mauricio.async.db.postgresql.messages.backend.AuthenticationResponseType
import com.github.mauricio.async.db.postgresql.messages.backend.ServerMessage


class CredentialMessage(val username: String,
                        val password: String,
                        val authenticationType: AuthenticationResponseType,
                        val salt: Array<Byte>?) : ClientMessage(ServerMessage.PasswordMessage)