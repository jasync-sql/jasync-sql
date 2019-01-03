package com.github.jasync.sql.db.pool

/**
 *
 * Raised when a pool has reached it's limit of available objects.
 *
 * @param message
 */
@Suppress("RedundantVisibilityModifier")
public class PoolExhaustedException(message: String) : IllegalStateException(message)
