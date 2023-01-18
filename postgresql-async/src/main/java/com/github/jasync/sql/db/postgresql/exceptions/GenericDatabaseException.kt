package com.github.jasync.sql.db.postgresql.exceptions

import com.github.jasync.sql.db.exceptions.DatabaseException
import com.github.jasync.sql.db.postgresql.messages.backend.ErrorMessage
import com.github.jasync.sql.db.postgresql.messages.backend.InformationMessage

class GenericDatabaseException(val errorMessage: ErrorMessage) : DatabaseException(errorMessage.toString()) {

    /**
     * get SQLSTATEinfo
     */
    fun getSQLState(): String? {
        val sqlStateField = errorMessage.fields.keys.firstOrNull { "SQLSTATE" == InformationMessage.fieldName(it) }
        return if (sqlStateField === null) null else errorMessage.fields[sqlStateField]
    }
}
