package com.github.jasync.r2dbc.mysql

import io.r2dbc.spi.R2dbcException

class JasyncDatabaseException(reason: String,
                              sqlState: String,
                              errorCode: Int,
                              cause: Throwable): R2dbcException(reason, sqlState, errorCode, cause)
