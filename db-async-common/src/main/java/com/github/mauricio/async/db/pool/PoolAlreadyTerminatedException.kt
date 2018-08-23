

package com.github.mauricio.async.db.pool

/**
 *
 * Thrown when the pool has already been closed.
 *
 */

class PoolAlreadyTerminatedException : IllegalStateException( "This pool has already been terminated" )