package com.github.mauricio.async.db.postgresql.exceptions

import com.github.jasync.sql.db.exceptions.DatabaseException

class InvalidArrayException(message: String) : DatabaseException(message)