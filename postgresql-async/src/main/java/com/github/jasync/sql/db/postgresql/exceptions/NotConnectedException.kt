package com.github.jasync.sql.db.postgresql.exceptions

import com.github.jasync.sql.db.exceptions.DatabaseException

@Suppress("unused")
class NotConnectedException(message: String) : DatabaseException(message)
