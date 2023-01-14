package com.github.jasync.r2dbc.mysql

import io.r2dbc.spi.R2dbcTransientException

class JasyncDatabaseException(
    reason: String,
    sqlState: String,
    errorCode: Int,
    cause: Throwable
) : R2dbcTransientException(reason, sqlState, errorCode, cause)
