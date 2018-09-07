package com.github.jasync.sql.db.postgresql.exceptions

import com.github.jasync.sql.db.exceptions.DatabaseException

class NotConnectedException(message: String) : DatabaseException(message)
