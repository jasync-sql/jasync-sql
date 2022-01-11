package com.github.jasync.sql.db.pool

import com.github.jasync.sql.db.util.Try
import com.github.jasync.sql.db.util.asCompletedFuture
import java.util.concurrent.CompletableFuture

/**
 *
 * Definition for objects that can be used as a factory for AsyncObjectPool
 * objects.
 * unlike ObjectFactory, in this interface methods are executed async so returning a future
 *
 * @tparam T the kind of object this factory produces.
 */

interface ObjectFactory<T> {

    /**
     *
     * Creates a valid object to be used in the pool.
     *
     * @return a future with created object
     */

    fun create(): CompletableFuture<out T>

    /**
     *
     * This method should "close" and release all resources acquired by the pooled object. This object will not be used
     * anymore so any cleanup necessary to remove it from memory should be made in this method. Implementors should not
     * raise an exception under any circumstances, the factory should log and clean up the exception itself.
     *
     * @param item
     */

    fun destroy(item: T)

    /**
     *
     * Validates that an object can still be used for it's purpose. This method should test the object to make sure
     * it's still valid for clients to use. If you have a database connection, test if you are still connected, if you're
     * accessing a file system, make sure you can still see and change the file.
     *
     * You decide how fast this method should return and what it will test, you should usually do something that's fast
     * enough not to slow down the pool usage, since this call will be made whenever an object taken/returns to the pool.
     *
     * @param item an object produced by this pool
     * @return If this object is not valid anymore, a Failure should be returned, otherwise Success
     *         should be the result of this call.
     */

    fun validate(item: T): Try<T>

    /**
     *
     * Does a full test on the given object making sure it's still valid. Different than validate, that's called whenever
     * an object is given back to the pool and should usually be fast, this method will be called when objects are
     * idle to make sure they don't "timeout" or become stale in anyway.
     *
     * @param item an object produced by this pool
     * @return a future with the object or a failed future in case test failed
     */

    fun test(item: T): CompletableFuture<T> = validate(item).asCompletedFuture()
}
