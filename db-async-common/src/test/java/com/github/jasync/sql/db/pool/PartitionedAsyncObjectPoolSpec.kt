package com.github.jasync.sql.db.pool

import com.github.jasync.sql.db.util.Try
import java.util.concurrent.atomic.AtomicInteger

class PartitionedAsyncObjectPoolSpec {

    val config = PoolConfiguration(100, Long.MAX_VALUE, 100, 5000)
    val current = AtomicInteger(0)
    val factory = object : ObjectFactory<Int> {
        val reject = HashSet<Int>()
        val failCreate = false

        override fun create(): Int {
            if (failCreate) {
                throw IllegalStateException("")
            }
            return current.incrementAndGet()
        }

        override fun destroy(item: Int) {
        }

        override fun validate(item: Int): Try<Int> {
            if (reject.contains(item)) {
                throw IllegalStateException("")
            }
            return Try.just(item)
        }
    }
}
/*
val pool = PartitionedAsyncObjectPool(factory, config, 2)
def maxObjects = config.getMaxObjects / 2
def maxIdle = config.maxIdle / 2
def maxQueueSize = config.maxQueueSize / 2

"pool contents" >> {

    "before exceed maxObjects" >> {

        "take one element" in {
            takeAndWait(1)

            pool.inUse.size mustEqual 1
            pool.queued.size mustEqual 0
            pool.availables.size mustEqual 0
        }

        "take one element and return it invalid" in {
            takeAndWait(1)
            factory.reject += 1

            await(pool.giveBack(1)) must throwA[IllegalStateException]

            pool.inUse.size mustEqual 0
            pool.queued.size mustEqual 0
            pool.availables.size mustEqual 0
        }

        "take one failed element" in {
            factory.failCreate = true
            takeAndWait(1) must throwA[IllegalStateException]

            pool.inUse.size mustEqual 0
            pool.queued.size mustEqual 0
            pool.availables.size mustEqual 0
        }

        "take maxObjects" in {
            takeAndWait(maxObjects)

            pool.inUse.size mustEqual maxObjects
            pool.queued.size mustEqual 0
            pool.availables.size mustEqual 0
        }

        "take maxObjects - 1 and take one failed" in {
            takeAndWait(maxObjects - 1)

            factory.failCreate = true
            takeAndWait(1) must throwA[IllegalStateException]

            pool.inUse.size mustEqual maxObjects - 1
            pool.queued.size mustEqual 0
            pool.availables.size mustEqual 0
        }

        "take maxObjects and receive one back" in {
            takeAndWait(maxObjects)
            await(pool.giveBack(1))

            pool.inUse.size mustEqual maxObjects - 1
            pool.queued.size mustEqual 0
            pool.availables.size mustEqual 1
        }

        "take maxObjects and receive one invalid back" in {
            takeAndWait(maxObjects)
            factory.reject += 1
            await(pool.giveBack(1)) must throwA[IllegalStateException]

            pool.inUse.size mustEqual maxObjects - 1
            pool.queued.size mustEqual 0
            pool.availables.size mustEqual 0
        }
    }

    "after exceed maxObjects" >> {

        takeAndWait(maxObjects)

        "before exceed maxQueueSize" >> {

            "one take queued" in {
                pool.take

                pool.inUse.size mustEqual maxObjects
                pool.queued.size mustEqual 1
                pool.availables.size mustEqual 0
            }

            "one take queued and receive one item back" in {
                val taking = pool.take

                await(pool.giveBack(1))

                await(taking) mustEqual 1
                pool.inUse.size mustEqual maxObjects
                pool.queued.size mustEqual 0
                pool.availables.size mustEqual 0
            }

            "one take queued and receive one invalid item back" in {
                val taking = pool.take
                factory.reject += 1
                await(pool.giveBack(1)) must throwA[IllegalStateException]

                pool.inUse.size mustEqual maxObjects - 1
                pool.queued.size mustEqual 1
                pool.availables.size mustEqual 0
            }

            "maxQueueSize takes queued" in {
                for (_ <- 0 until maxQueueSize)
                    pool.take

                pool.inUse.size mustEqual maxObjects
                pool.queued.size mustEqual maxQueueSize
                pool.availables.size mustEqual 0
            }

            "maxQueueSize takes queued and receive one back" in {
                val taking = pool.take
                for (_ <- 0 until maxQueueSize - 1)
                    pool.take

                await(pool.giveBack(10))

                await(taking) mustEqual 10
                pool.inUse.size mustEqual maxObjects
                pool.queued.size mustEqual maxQueueSize - 1
                pool.availables.size mustEqual 0
            }

            "maxQueueSize takes queued and receive one invalid back" in {
                for (_ <- 0 until maxQueueSize)
                    pool.take

                factory.reject += 11
                await(pool.giveBack(11)) must throwA[IllegalStateException]

                pool.inUse.size mustEqual maxObjects - 1
                pool.queued.size mustEqual maxQueueSize
                pool.availables.size mustEqual 0
            }
        }

        "after exceed maxQueueSize" >> {

            for (_ <- 0 until maxQueueSize)
                pool.take

            "start to reject takes" in {
                await(pool.take) must throwA[PoolExhaustedException]

                pool.inUse.size mustEqual maxObjects
                pool.queued.size mustEqual maxQueueSize
                pool.availables.size mustEqual 0
            }

            "receive an object back" in {
                await(pool.giveBack(1))

                pool.inUse.size mustEqual maxObjects
                pool.queued.size mustEqual maxQueueSize - 1
                pool.availables.size mustEqual 0
            }

            "receive an invalid object back" in {
                factory.reject += 1
                await(pool.giveBack(1)) must throwA[IllegalStateException]

                pool.inUse.size mustEqual maxObjects - 1
                pool.queued.size mustEqual maxQueueSize
                pool.availables.size mustEqual 0
            }

            "receive maxQueueSize objects back" in {
                for (i <- 1 to maxQueueSize)
                    await(pool.giveBack(i))

                pool.inUse.size mustEqual maxObjects
                pool.queued.size mustEqual 0
                pool.availables.size mustEqual 0
            }

            "receive maxQueueSize invalid objects back" in {
                for (i <- 1 to maxQueueSize) {
                    factory.reject += i
                    await(pool.giveBack(i)) must throwA[IllegalStateException]
                }

                pool.inUse.size mustEqual maxObjects - maxQueueSize
                pool.queued.size mustEqual maxQueueSize
                pool.availables.size mustEqual 0
            }

            "receive maxQueueSize + 1 object back" in {
                for (i <- 1 to maxQueueSize)
                    await(pool.giveBack(i))

                await(pool.giveBack(1))
                pool.inUse.size mustEqual maxObjects - 1
                pool.queued.size mustEqual 0
                pool.availables.size mustEqual 1
            }

            "receive maxQueueSize + 1 invalid object back" in {
                for (i <- 1 to maxQueueSize)
                    await(pool.giveBack(i))

                factory.reject += 1
                await(pool.giveBack(1)) must throwA[IllegalStateException]
                pool.inUse.size mustEqual maxObjects - 1
                pool.queued.size mustEqual 0
                pool.availables.size mustEqual 0
            }
        }
    }
}

"gives back the connection to the original pool" in {
    val executor = Executors.newFixedThreadPool(20)
    implicit val context = ExecutionContext.fromExecutor(executor)

    val takes =
        for (_ <- 0 until 30) yield {
            Future().flatMap(_ => pool.take)
        }
    val takesAndReturns =
        Future.sequence(takes).flatMap { items =>
            Future.sequence(items.map(pool.giveBack))
        }

    await(takesAndReturns)

    executor.shutdown
    pool.inUse.size mustEqual 0
    pool.queued.size mustEqual 0
    pool.availables.size mustEqual 30
}

private def takeAndWait(objects: Int) =
    for (_ <- 0 until objects)
        await(pool.take)

private def await[T](future: Future[T]) =
    Await.result(future, Duration.Inf)

}
*/
