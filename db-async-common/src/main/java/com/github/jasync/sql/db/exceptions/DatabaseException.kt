package com.github.jasync.sql.db.exceptions

open class DatabaseException : RuntimeException {

    constructor(message: String) : super(message)

    constructor(message: String, cause: Throwable) : super(message, cause)

}
