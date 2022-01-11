package com.github.jasync.sql.db.pool

/**
 *
 * Thrown when the pool has already been closed.
 *
 */
@Suppress("RedundantVisibilityModifier")
public class PoolAlreadyTerminatedException : IllegalStateException("This pool has already been terminated")
