package com.github.mauricio.async.db.pool

//import scala.concurrent.Future
//import com.github.mauricio.async.db.util.ExecutorServiceUtils
//import io.github.vjames19.futures.jdk8.map
//import scala.concurrent.Promise
//import java.util.concurrent.ConcurrentHashMap
//import scala.util.Success
//import scala.util.Failure

//not in use
class PartitionedAsyncObjectPool<T>()
//    factory: ObjectFactory<T>,
//    configuration: PoolConfiguration,
//    numberOfPartitions: Int)
//    : AsyncObjectPool<T> {
//
//    import ExecutorServiceUtils.CachedExecutionContext
//
//    private val pools =
//        (0 until numberOfPartitions)
//            .map(_ -> SingleThreadedAsyncObjectPool(factory, partitionConfig))
//            .toMap
//
//    private val checkouts = ConcurrentHashMap<T, SingleThreadedAsyncObjectPool<T>>
//
//    fun take: Future<T> {
//        val pool = currentPool
//        pool.take.andThen {
//            Success(conn) ->
//                checkouts.put(conn, pool)
//            Failure(_) ->
//        }
//    }
//
//    fun giveBack(item: T) =
//        checkouts
//            .remove(item)
//            .giveBack(item)
//            .map(_ -> this)
//
//    fun close ()=
//        Future.sequence(pools.values.map(_.close)).map {
//            _ -> this
//        }
//
//    fun availables: Traversable<T> = pools.values.map(_.availables).flatten
//
//    fun inUse: Traversable<T> = pools.values.map(_.inUse).flatten
//
//    fun queued: Traversable<Promise<T>> = pools.values.map(_.queued).flatten
//
//    protected fun isClosed ()=
//        pools.values.forall(_.isClosed)
//
//    private fun currentPool ()=
//        pools(currentThreadAffinity)
//
//    private fun currentThreadAffinity ()=
//        (Thread.currentThread.getId % numberOfPartitions).toInt
//
//    private fun partitionConfig ()=
//        configuration.copy(
//            maxObjects = configuration.maxObjects / numberOfPartitions,
//            maxQueueSize = configuration.maxQueueSize / numberOfPartitions
//        )
//}
