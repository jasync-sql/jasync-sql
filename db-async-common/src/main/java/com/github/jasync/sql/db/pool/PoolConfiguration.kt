
package com.github.jasync.sql.db.pool

/**
 *
 * Defines specific pieces of a pool's behavior.
 *
 * @param maxObjects how many objects this pool will hold
 * @param maxIdle number of milliseconds for which the objects are going to be kept as idle (not in use by clients of the pool)
 * @param maxQueueSize when there are no more objects, the pool can queue up requests to serve later then there
 *                     are objects available, this is the maximum number of enqueued requests
 * @param validationInterval pools will use this value as the timer period to validate idle objects.
 * @param createTimeout the timeout for connecting to servers
 * @param testTimeout the timeout for connection tests performed by pools
 */

data class PoolConfiguration(
    val maxObjects: Int,
    val maxIdle: Long,
    val maxQueueSize: Int,
    val validationInterval: Long = 5000,
    val createTimeout: Long = 5000,
    val testTimeout: Long = 5000
    )
{
  companion object {
    val Default = PoolConfiguration(10, 4, 10)
  }
}
