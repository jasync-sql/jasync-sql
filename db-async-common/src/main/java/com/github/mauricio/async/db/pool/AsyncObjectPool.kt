package com.github.mauricio.async.db.pool

import io.github.vjames19.futures.jdk8.flatMap
import io.github.vjames19.futures.jdk8.onComplete
import java.util.concurrent.CompletableFuture

/**
 *
 * Defines the common interface for async object pools. These are pools that do not block clients trying to acquire
 * a resource from it. Different than the usual synchronous pool, you **must** return objects back to it manually
 * since it's impossible for the pool to know when the object is ready to be given back.
 *
 * @tparam T
 */

interface AsyncObjectPool<T> {

  /**
   *
   * Returns an object from the pool to the callee , the returned future. If the pool can not create or enqueue
   * requests it will fill the returned <<scala.concurrent.Future>> , an
   * <<com.github.mauricio.async.db.pool.PoolExhaustedException>>.
   *
   * @return future that will eventually return a usable pool object.
   */

  fun take(): CompletableFuture<T>

  /**
   *
   * Returns an object taken from the pool back to it. This object will become available for another client to use.
   * If the object is invalid or can not be reused for some reason the <<scala.concurrent.Future>> returned will contain
   * the error that prevented this object of being added back to the pool. The object is then discarded from the pool.
   *
   * @param item
   * @return
   */

  fun giveBack(item: T): CompletableFuture<AsyncObjectPool<T>>

  /**
   *
   * Closes this pool and future calls to **take** will cause the <<scala.concurrent.Future>> to raise an
   * <<com.github.mauricio.async.db.pool.PoolAlreadyTerminatedException>>.
   *
   * @return
   */

  fun close(): CompletableFuture<AsyncObjectPool<T>>

  /**
   *
   * Retrieve and use an object from the pool for a single computation, returning it when the operation completes.
   *
   * @param function function that uses the object
   * @return function wrapped , take and giveBack
   */

  fun <A> use(function: (T) -> CompletableFuture<A>): CompletableFuture<A> = //TODO (implicit executionContext: ExecutionContext)
      take().flatMap { item ->
        val future = CompletableFuture<A>()
        function(item).onComplete(
            onFailure = { error ->
              giveBack(item).onComplete(
                  onFailure = { _ ->
                    future.completeExceptionally(error)
                  },
                  onSuccess = { result ->
                    future.completeExceptionally(error)
                  }
              )
            },
            onSuccess = { r1 ->
              giveBack(item).onComplete(
                  onFailure = { _ ->
                    //TODO add log message
                    future.complete(r1)
                  },
                  onSuccess = { result ->
                    future.complete(r1)
                  }
              )
            }
        )
      }

}
