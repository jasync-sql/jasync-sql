package com.github.mauricio.async.db.postgresql.exceptions

import com.github.jasync.sql.db.exceptions.DatabaseException
import com.github.mauricio.async.db.postgresql.messages.backend.AuthenticationResponseType

class MissingCredentialInformationException(
    val username: String,
    val password: String?,
    private val authenticationResponseType: AuthenticationResponseType)
  : DatabaseException(
    "Username and password were required by auth type %s but are not available (username=<%s> password=<%s>".format(
        authenticationResponseType,
        username,
        password
    )
)