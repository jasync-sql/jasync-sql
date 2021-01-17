package com.github.jasync.sql.db.postgresql.exceptions

import com.github.jasync.sql.db.exceptions.DatabaseException

class MissingCredentialInformationException(
    val username: String,
    val password: String?
) : DatabaseException(
    "Username and password were required but are not available " +
        "(username=<$username> password ${password?.let { "provided" } ?: "not provided"})"
)
