package com.github.jasync.sql.db.postgresql.exceptions

import com.github.jasync.sql.db.exceptions.DatabaseException

class AuthenticationException(message: String) : DatabaseException("Authentication exception: $message")
