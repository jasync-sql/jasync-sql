package com.github.jasync.sql.db

import com.github.jasync.sql.db.pool.PooledObject

/**
 * An interface represents a connection driver (not a wrapper)
 */
interface ConcreteConnection : Connection, PooledObject
