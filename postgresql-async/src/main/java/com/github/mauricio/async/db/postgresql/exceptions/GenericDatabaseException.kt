package com.github.mauricio.async.db.postgresql.exceptions

import com.github.jasync.sql.db.exceptions.DatabaseException
import com.github.mauricio.async.db.postgresql.messages.backend.ErrorMessage

class GenericDatabaseException(errorMessage: ErrorMessage) : DatabaseException(errorMessage.toString())