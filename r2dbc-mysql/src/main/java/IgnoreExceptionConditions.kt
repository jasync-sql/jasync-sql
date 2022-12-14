package com.github.jasync.r2dbc.mysql

import com.github.jasync.sql.db.exceptions.ConnectionStillRunningQueryException

fun isRollbackQueryConflictException(sql: String, exception: Throwable): Boolean {
    return sql == "ROLLBACK" && exception is ConnectionStillRunningQueryException
}
