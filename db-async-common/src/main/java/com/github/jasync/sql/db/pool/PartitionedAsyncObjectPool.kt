package com.github.jasync.sql.db.pool

import com.github.jasync.sql.db.util.ExecutorServiceUtils
import com.github.jasync.sql.db.util.mapAsync
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor


open class PartitionedAsyncObjectPool<T>(
    factory: ObjectFactory<T>,
    private val configuration: PoolConfiguration,
    private val numberOfPartitions: Int,
    private val executionContext: Executor = ExecutorServiceUtils.CommonPool)
  : AsyncObjectPool<T> {

  private val pools: Map<Int, SingleThreadedAsyncObjectPool<T>> =
      (0 until numberOfPartitions)
          .map { it to SingleThreadedAsyncObjectPool(factory, partitionConfig()) }.toMap()


  private val checkouts = ConcurrentHashMap<T, SingleThreadedAsyncObjectPool<T>>()

  override fun take(): CompletableFuture<T> {
    val pool = currentPool()
    return pool.take().mapAsync(executionContext) {
      checkouts[it] = pool
      it
    }
  }

  override fun giveBack(item: T): CompletableFuture<AsyncObjectPool<T>> {
    val removed = checkouts.remove(item)!!
    val singleRemoved = removed.giveBack(item)
    return singleRemoved.mapAsync(executionContext) { this }
  }

  override fun close(): CompletableFuture<AsyncObjectPool<T>> =
      CompletableFuture.allOf(* pools.values.map { it.close() }.toTypedArray()).mapAsync(executionContext) { this }

  fun availables(): List<T> = pools.values.map { it.availables() }.flatten()

  fun inUse(): List<T> = pools.values.map { it.inUse() }.flatten()

  fun queued(): List<CompletableFuture<T>> = pools.values.map { it.queued() }.flatten()

  protected fun isClosed() =
      pools.values.all { it.isClosed() }

  private fun currentPool() = pools.getValue(currentThreadAffinity())

  private fun currentThreadAffinity() = (Thread.currentThread().getId() % numberOfPartitions).toInt()

  private fun partitionConfig() =
      configuration.copy(
          maxObjects = configuration.maxObjects / numberOfPartitions,
          maxQueueSize = configuration.maxQueueSize / numberOfPartitions
      )
}
