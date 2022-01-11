package com.github.jasync.sql.db.postgresql.exceptions

import com.github.jasync.sql.db.exceptions.DatabaseException
import com.github.jasync.sql.db.postgresql.messages.backend.ErrorMessage

class GenericDatabaseException(val errorMessage: ErrorMessage) : DatabaseException(errorMessage.toString())
