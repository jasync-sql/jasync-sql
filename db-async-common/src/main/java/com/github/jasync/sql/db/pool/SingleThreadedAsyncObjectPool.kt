package com.github.jasync.sql.db.pool

import com.github.jasync.sql.db.util.Failure
import com.github.jasync.sql.db.util.FuturePromise
import com.github.jasync.sql.db.util.Log
import com.github.jasync.sql.db.util.Success
import com.github.jasync.sql.db.util.Worker
import com.github.jasync.sql.db.util.failed
import com.github.jasync.sql.db.util.failure
import com.github.jasync.sql.db.util.headTail
import com.github.jasync.sql.db.util.success
import io.netty.util.concurrent.FastThreadLocal.removeAll
import io.netty.util.concurrent.Promise
import mu.KotlinLogging
import java.util.LinkedList
import java.util.Queue
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.CompletableFuture
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicLong

/**
 *
 * Implements an <<com.github.mauricio.sql.db.pool.AsyncObjectPool>> using a single thread from a
 * fixed executor service as an event loop to cause all calls to be sequential.
 *
 * Once you are done , this object remember to call it's close method to clean up the thread pool and
 * it's objects as this might prevent your application from ending.
 *
 * @param factory
 * @param configuration
 * @tparam T type of the object this pool holds
 */

open class SingleThreadedAsyncObjectPool<T>(
    private val factory: ObjectFactory<T>,
    private val configuration: PoolConfiguration
) : AsyncObjectPool<T> {

  companion object {
    val Counter = AtomicLong()
  }

  private val mainPool = Worker()
  private var poolables = emptyList<PoolableHolder<T>>()
  private val checkouts = mutableListOf<T>() // configuration.maxObjects
  private val waitQueue: Queue<CompletableFuture<T>> = LinkedList<CompletableFuture<T>>()
  private val timer = Timer("sql-object-pool-timer-" + Counter.incrementAndGet(), true)

  init {
    timer.scheduleAtFixedRate(object : TimerTask() {
      override fun run() {
        mainPool.action {
          testObjects()
        }
      }
    }, configuration.validationInterval, configuration.validationInterval)
  }

  private var closed = false

  /**
   *
   * Asks for an object from the pool, this object should be returned to the pool when not in use anymore.
   *
   * @return
   */

  override fun take(): CompletableFuture<T> {

    if (this.closed) {
      return CompletableFuture<T>().failed(PoolAlreadyTerminatedException())
    }

    val promise = CompletableFuture<T>()
    this.checkout(promise)
    return promise
  }

  /**
   *
   * Returns an object to the pool. The object is validated before being added to the collection
   * of available objects to make sure we have a usable object. If the object isn't valid it's discarded.
   *
   * @param item
   * @return
   */

  override fun giveBack(item: T): CompletableFuture<AsyncObjectPool<T>> {
    val promise = CompletableFuture<AsyncObjectPool<T>>()
    this.mainPool.action {
      // Ensure it came from this pool
      val idx = this.checkouts.indexOf(item)
      if (idx >= 0) {
        this.checkouts.removeAt(idx)
        val validated = this.factory.validate(item)
        when (validated) {
          is Success ->
            this.addBack(item, promise)
          is Failure -> {
            this.factory.destroy(item)
            promise.failure(validated.exception)
          }
        }
      } else {
        // It's already a failure but lets doublecheck why
        val isFromOurPool: Boolean = this.poolables.any { holder -> item == holder.item }
        if (isFromOurPool) {
          promise.failure(IllegalStateException("This item has already been returned"))
        } else {
          promise.failure(IllegalArgumentException("The returned item did not come from this pool."))
        }
      }
    }

    return promise
  }

  fun isFull(): Boolean = this.poolables.isEmpty() && this.checkouts.size == configuration.maxObjects

  override fun close(): CompletableFuture<AsyncObjectPool<T>> {
    return try {
      val promise = CompletableFuture<AsyncObjectPool<T>>()
      this.mainPool.action {
        if (!this.closed) {
          try {
            this.timer.cancel()
            this.mainPool.shutdown()
            this.closed = true
            (this.poolables.map { i -> i.item } + this.checkouts).forEach { item -> factory.destroy(item) }
            promise.success(this)
          } catch (e: Exception) {
            promise.failure(e)
          }
        } else {
          promise.success(this)
        }
      }
      promise
    } catch (e: RejectedExecutionException) {
      if (this.closed) {
        FuturePromise.successful(this)
      } else throw e
    }
  }

  fun availables(): List<T> = this.poolables.map { item -> item.item }

  fun inUse(): List<T> = this.checkouts

  fun queued(): Queue<CompletableFuture<T>> = this.waitQueue

  fun isClosed(): Boolean = this.closed

  /**
   *
   * Adds back an object that was in use to the list of poolable objects.
   *
   * @param item
   * @param promise
   */

  private fun addBack(item: T, promise: CompletableFuture<AsyncObjectPool<T>>) {
    this.poolables = this.poolables + PoolableHolder<T>(item)

    if (this.waitQueue.isNotEmpty()) {
      this.checkout(this.waitQueue.remove())
    }

    promise.success(this)
  }

  /**
   *
   * Enqueues a promise to be fulfilled in the future when objects are sent back to the pool. If
   * we have already reached the limit of enqueued objects, fail the promise.
   *
   * @param promise
   */

  private fun enqueuePromise(promise: CompletableFuture<T>) {
    if (this.waitQueue.size >= configuration.maxQueueSize) {
      val exception = PoolExhaustedException("There are no objects available and the waitQueue is full")
      exception.fillInStackTrace()
      promise.completeExceptionally(exception)
    } else {
      this.waitQueue += promise
    }
  }

  private fun checkout(promise: CompletableFuture<T>) {
    this.mainPool.action {
      if (this.isFull()) {
        this.enqueuePromise(promise)
      } else {
        this.createOrReturnItem(promise)
      }
    }
  }

  /**
   *
   * Checks if there is a poolable object available and returns it to the promise.
   * If there are no objects available, create a one using the factory and return it.
   *
   * @param promise
   */

  private fun createOrReturnItem(promise: CompletableFuture<T>) {
    if (this.poolables.isEmpty()) {
      try {
        val item = this.factory.create()
        this.checkouts += item
        promise.complete(item)
      } catch (e: Exception) {
        promise.completeExceptionally(e)
      }
    } else {
      val (h, t) = this.poolables.headTail
      this.poolables = t
      val item = h.item
      this.checkouts += item
      promise.success(item)
    }
  }

  fun finalize() { //according to kotlin docs override is not needed: https://kotlinlang.org/docs/reference/java-interop.html#finalize
    this.close()
  }

  /**
   *
   * Validates pooled objects not in use to make sure they are all usable, great if
   * you're holding onto network connections since you can "ping" the destination
   * to keep the connection alive.
   *
   */

  private fun testObjects() {
    val removals = mutableListOf<PoolableHolder<T>>()
    this.poolables.forEach { poolable ->
      val tested = this.factory.test(poolable.item)
      when {
        tested.isSuccess -> {
          if (poolable.timeElapsed() > configuration.maxIdle) {
            logger.debug("Connection was idle for {}, maxIdle is {}, removing it", poolable.timeElapsed(), configuration.maxIdle)
            removals += poolable
            factory.destroy(poolable.item)
          }
        }
        else -> {
          logger.error("Failed to validate object", (tested as Failure).exception)
          removals += poolable
          factory.destroy(poolable.item)
        }
      }
    }
    this.poolables = this.poolables.toMutableList().also {
      it.removeAll(removals)
    }
  }

  private class PoolableHolder<T>(val item: T) {
    val time = System.currentTimeMillis()

    fun timeElapsed() = System.currentTimeMillis() - time
  }

}

private val logger = KotlinLogging.logger {}

