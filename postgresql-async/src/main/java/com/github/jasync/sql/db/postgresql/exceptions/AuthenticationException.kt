package com.github.jasync.sql.db.postgresql.exceptions

import com.github.jasync.sql.db.exceptions.DatabaseException

class AuthenticationException : DatabaseException {
    constructor(message: String) : super("Authentication exception: $message")
    constructor(message: String, cause: Throwable) : super("Authentication exception: $message", cause)
}
