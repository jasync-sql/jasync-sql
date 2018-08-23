package com.github.mauricio.async.db.pool

import scala.concurrent.Future
import com.github.mauricio.async.db.util.ExecutorServiceUtils
import scala.concurrent.Promise
import java.util.concurrent.ConcurrentHashMap
import scala.util.Success
import scala.util.Failure

class PartitionedAsyncObjectPool[T](
    factory: ObjectFactory[T],
    configuration: PoolConfiguration,
    numberOfPartitions: Int)
    extends AsyncObjectPool[T] {

    import ExecutorServiceUtils.CachedExecutionContext

    private val pools =
        (0 until numberOfPartitions)
            .map(_ -> new SingleThreadedAsyncObjectPool(factory, partitionConfig))
            .toMap

    private val checkouts = new ConcurrentHashMap[T, SingleThreadedAsyncObjectPool[T]]

    def take: Future[T] = {
        val pool = currentPool
        pool.take.andThen {
            case Success(conn) =>
                checkouts.put(conn, pool)
            case Failure(_) =>
        }
    }

    def giveBack(item: T) =
        checkouts
            .remove(item)
            .giveBack(item)
            .map(_ => this)

    def close =
        Future.sequence(pools.values.map(_.close)).map {
            _ => this
        }

    def availables: Traversable[T] = pools.values.map(_.availables).flatten

    def inUse: Traversable[T] = pools.values.map(_.inUse).flatten

    def queued: Traversable[Promise[T]] = pools.values.map(_.queued).flatten

    protected def isClosed =
        pools.values.forall(_.isClosed)

    private def currentPool =
        pools(currentThreadAffinity)

    private def currentThreadAffinity =
        (Thread.currentThread.getId % numberOfPartitions).toInt

    private def partitionConfig =
        configuration.copy(
            maxObjects = configuration.maxObjects / numberOfPartitions,
            maxQueueSize = configuration.maxQueueSize / numberOfPartitions
        )
}