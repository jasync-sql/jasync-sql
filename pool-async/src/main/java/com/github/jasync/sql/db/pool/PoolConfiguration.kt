package com.github.jasync.sql.db.pool

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 *
 * Defines specific pieces of a pool's behavior.
 *
 * @param maxObjects how many objects this pool will hold
 * @param maxIdle number of milliseconds for which the objects are going to be kept as idle (not in use by clients of the pool)
 * @param maxQueueSize when there are no more objects, the pool can queue up requests to serve later then there
 *                     are objects available, this is the maximum number of enqueued requests
 * @param maxObjectTtl max time to live for connections, null if infinite
 * @param validationInterval pools will use this value as the timer period to validate idle objects.
 * @param createTimeout the timeout for connecting to servers
 * @param testTimeout the timeout for connection tests performed by pools
 * @param queryTimeout the optional query timeout
 * @param coroutineDispatcher thread pool for the actor operations of the connection pool
 * @param minObjects the minimum number of objects this pool should hold
 */

data class PoolConfiguration @JvmOverloads constructor(
    val maxObjects: Int,
    val maxIdle: Long,
    val maxQueueSize: Int,
    val validationInterval: Long = 5000,
    val createTimeout: Long = 10000, // It is suggested to set this to sql.db.Configuration.connectionTimeout * 2
    val testTimeout: Long = 5000,
    val queryTimeout: Long? = null,
    val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
    val maxObjectTtl: Long? = null,
    val minObjects: Int? = null
) {
    companion object {
        @Suppress("unused")
        val Default = PoolConfiguration(30, 10, 100000)
    }
}
