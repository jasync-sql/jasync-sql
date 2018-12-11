package com.github.jasync.sql.db.pool

/**
 *
 * Raised when a pool has reached it's limit of available objects.
 *
 * @param message
 */

class PoolExhaustedException(message: String) : IllegalStateException(message)
