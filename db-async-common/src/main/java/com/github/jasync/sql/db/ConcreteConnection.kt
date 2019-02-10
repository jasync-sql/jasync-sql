package com.github.jasync.sql.db

import com.github.jasync.sql.db.pool.PooledObject
import com.github.jasync.sql.db.pool.TimeoutScheduler

/**
 * An interface represents a connection driver (not a wrapper)
 */
interface ConcreteConnection : Connection, PooledObject, TimeoutScheduler {

    fun isQuerying(): Boolean

    fun lastException(): Throwable?
}
