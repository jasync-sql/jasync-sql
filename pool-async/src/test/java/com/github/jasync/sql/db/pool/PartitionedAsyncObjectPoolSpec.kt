package com.github.jasync.sql.db.pool

import com.github.jasync.sql.db.util.FP
import com.github.jasync.sql.db.util.Try
import com.github.jasync.sql.db.util.flatMapAsync
import com.github.jasync.sql.db.util.mapAsync
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.After
import org.junit.Test

class PartitionedAsyncObjectPoolSpec {

    private val config = PoolConfiguration(100, Long.MAX_VALUE, 100)
    private val factory = ForTestingObjectFactory()

    private var tested =
        ActorBasedObjectPool(
            factory,
            config,
            testItemsPeriodically = false)

    private val pool = tested
    private val maxObjects = config.maxObjects

    // val maxIdle = config.maxIdle / 2
    private val maxQueueSize = config.maxQueueSize

    private class ForTestingObjectFactory :
        ObjectFactory<MyPooledObject> {
        val reject = HashSet<MyPooledObject>()
        var failCreate = false
        val current = AtomicInteger(0)
        val createdObjects = mutableListOf<MyPooledObject>()

        override fun create(): CompletableFuture<MyPooledObject> =
            if (failCreate) {
                FP.failed(IllegalStateException("failed to create item (it is intentional)"))
            } else {
                val created = MyPooledObject(current.incrementAndGet())
                createdObjects.add(created)
                FP.successful(created)
            }

        override fun destroy(item: MyPooledObject) {
        }

        override fun validate(item: MyPooledObject): Try<MyPooledObject> {
            if (reject.contains(item)) {
                throw IllegalStateException("validate failed for the test (it is intentional)")
            }
            return Try.just(item)
        }
    }

    private val takenObjects = mutableListOf<MyPooledObject>()
    private val queuedObjects = mutableListOf<CompletableFuture<MyPooledObject>>()

    private fun takeAndWait(objects: Int) {
        for (it in 1..objects) {
            takenObjects += pool.take().get()
        }
    }

    private fun takeQueued(objects: Int) {
        takeNoWait(objects)
        await.untilCallTo { pool.waitingForItem.size } matches { it == objects }
    }

    private fun takeNoWait(objects: Int) {
        for (it in 1..objects) {
            queuedObjects += pool.take()
        }
    }

    @After
    fun closePool() {
        tested.close().get()
    }

    @Test
    fun `pool contents - before exceed maxObjects - take one element`() {
        takeAndWait(1)
        assertThat(pool.usedItems.size).isEqualTo(1)
        assertThat(pool.waitingForItem.size).isEqualTo(0)
        assertThat(pool.availableItems.size).isEqualTo(0)
    }

    private fun verifyException(
        exType: Class<out java.lang.Exception>,
        causeType: Class<out java.lang.Exception>? = null,
        body: () -> Unit
    ) {
        try {
            body()
            throw Exception("${exType.simpleName}->${causeType?.simpleName} was not thrown")
        } catch (e: Exception) {
            e.printStackTrace()
            assertThat(e::class.java).isEqualTo(exType)
            var cause = e.cause
            while (cause?.cause != null) {
                cause = cause.cause
            }
            causeType?.let { assertThat(cause!!::class.java).isEqualTo(it) }
        }
    }

    @Test
    fun `pool contents - before exceed maxObjects - take one element and return it invalid`() {
        takeAndWait(1)
        factory.reject += MyPooledObject(1)

        verifyException(ExecutionException::class.java, IllegalStateException::class.java) {
            pool.giveBack(MyPooledObject(1)).get()
        }

        assertThat(pool.usedItems.size).isEqualTo(0)
        assertThat(pool.waitingForItem.size).isEqualTo(0)
        assertThat(pool.availableItems.size).isEqualTo(0)
    }

    @Test
    fun `pool contents - before exceed maxObjects - take one failed element`() {
        factory.failCreate = true
        verifyException(ExecutionException::class.java, IllegalStateException::class.java) {
            takeAndWait(1)
        }
        assertThat(pool.usedItems.size).isEqualTo(0)
        assertThat(pool.waitingForItem.size).isEqualTo(0)
        assertThat(pool.availableItems.size).isEqualTo(0)
    }

    @Test
    fun `pool contents - before exceed maxObjects - take maxObjects`() {
        takeAndWait(maxObjects)

        assertThat(pool.usedItems.size).isEqualTo(maxObjects)
        assertThat(pool.waitingForItem.size).isEqualTo(0)
        assertThat(pool.availableItems.size).isEqualTo(0)
    }

    @Test
    fun `pool contents - before exceed maxObjects - take maxObjects - 1 and take one failed`() {
        takeAndWait(maxObjects - 1)

        factory.failCreate = true
        verifyException(ExecutionException::class.java, IllegalStateException::class.java) {
            takeAndWait(1)
        }
        assertThat(pool.usedItems.size).isEqualTo(maxObjects - 1)
        assertThat(pool.waitingForItem.size).isEqualTo(0)
        assertThat(pool.availableItems.size).isEqualTo(0)
    }

    @Test
    fun `pool contents - before exceed maxObjects - "take maxObjects and receive one back"`() {
        takeAndWait(maxObjects)
        pool.giveBack(MyPooledObject(1)).get()

        assertThat(pool.usedItems.size).isEqualTo(maxObjects - 1)
        assertThat(pool.waitingForItem.size).isEqualTo(0)
        await.untilCallTo { pool.availableItems.size } matches { it == 1 }
    }

    @Test
    fun `pool contents - before exceed maxObjects - "take maxObjects and receive one invalid back"`() {
        takeAndWait(maxObjects)
        factory.reject += MyPooledObject(1)
        verifyException(ExecutionException::class.java, IllegalStateException::class.java) {
            pool.giveBack(MyPooledObject(1)).get()
        }
        assertThat(pool.usedItems.size).isEqualTo(maxObjects - 1)
        assertThat(pool.waitingForItem.size).isEqualTo(0)
        assertThat(pool.availableItems.size).isEqualTo(0)
    }

    @Test
    fun `pool contents - after exceed maxObjects, before exceed maxQueueSize - "one take queued"`() {
        takeAndWait(maxObjects)
        takeQueued(1)

        assertThat(pool.usedItems.size).isEqualTo(maxObjects)
        assertThat(pool.waitingForItem.size).isEqualTo(1)
        assertThat(pool.availableItems.size).isEqualTo(0)
    }

    @Test
    fun `pool contents - after exceed maxObjects, before exceed maxQueueSize - "one take queued and receive one item back"`() {
        takeAndWait(maxObjects)

        val taking = pool.take()

        pool.giveBack(MyPooledObject(1)).get()

        assertThat(taking.get()).isEqualTo(1.toPoolObject)
        await.untilCallTo { pool.usedItems.size } matches { it == maxObjects }
        assertThat(pool.waitingForItem.size).isEqualTo(0)
        assertThat(pool.availableItems.size).isEqualTo(0)
    }

    private val Int.toPoolObject: MyPooledObject
        get() = MyPooledObject(this)

    @Test
    fun `pool contents - after exceed maxObjects, before exceed maxQueueSize - "one take queued and receive one invalid item back"`() {
        takeAndWait(maxObjects)

        pool.take()
        factory.reject += MyPooledObject(1)
        verifyException(ExecutionException::class.java, IllegalStateException::class.java) {
            pool.giveBack(MyPooledObject(1)).get()
        }

        await.untilCallTo { pool.usedItems.size }.matches { it == maxObjects }
        assertThat(pool.waitingForItem.size).isEqualTo(0)
        assertThat(pool.availableItems.size).isEqualTo(0)
    }

    @Test
    fun `pool contents - after exceed maxObjects, before exceed maxQueueSize - "maxQueueSize takes queued"`() {
        takeAndWait(maxObjects)
        takeQueued(maxQueueSize)

        assertThat(pool.usedItems.size).isEqualTo(maxObjects)
        assertThat(pool.waitingForItem.size).isEqualTo(maxQueueSize)
        assertThat(pool.availableItems.size).isEqualTo(0)
    }

    @Test
    fun `pool contents - after exceed maxObjects, before exceed maxQueueSize - "maxQueueSize takes queued and receive one back"`() {
        takeAndWait(maxObjects)

        val taking = pool.take()
        takeNoWait(maxQueueSize - 1)

        pool.giveBack(MyPooledObject(10)).get()

        assertThat((taking).get()).isEqualTo(10.toPoolObject)
        await.untilCallTo { pool.usedItems.size } matches { it == maxObjects }
        assertThat(pool.waitingForItem.size).isEqualTo(maxQueueSize - 1)
        assertThat(pool.availableItems.size).isEqualTo(0)
    }

    @Test
    fun `pool contents - after exceed maxObjects, before exceed maxQueueSize - "maxQueueSize takes queued and receive one invalid back"`() {
        takeAndWait(maxObjects)
        takeQueued(maxQueueSize)

        factory.reject += MyPooledObject(11)
        verifyException(ExecutionException::class.java, IllegalStateException::class.java) {
            pool.giveBack(MyPooledObject(11)).get()
        }

        await.untilCallTo { pool.usedItems.size } matches { it == maxObjects }
        assertThat(pool.waitingForItem.size).isEqualTo(maxQueueSize - 1)
        assertThat(pool.availableItems.size).isEqualTo(0)
    }

    @Test
    fun `pool contents - after exceed maxObjects, after exceed maxQueueSize - "start to reject takes"`() {
        takeAndWait(maxObjects)
        takeQueued(maxQueueSize)

        verifyException(ExecutionException::class.java, PoolExhaustedException::class.java) {
            (pool.take().get())
        }

        assertThat(pool.usedItems.size).isEqualTo(maxObjects)
        assertThat(pool.waitingForItem.size).isEqualTo(maxQueueSize)
        assertThat(pool.availableItems.size).isEqualTo(0)
    }

    @Test
    fun `pool contents - after exceed maxObjects, after exceed maxQueueSize - "receive an object back"`() {
        takeAndWait(maxObjects)
        takeQueued(maxQueueSize)

        (pool.giveBack(MyPooledObject(1))).get()

        await.untilCallTo { pool.usedItems.size } matches { it == maxObjects }
        assertThat(pool.waitingForItem.size).isEqualTo(maxQueueSize - 1)
        assertThat(pool.availableItems.size).isEqualTo(0)
    }

    @Test
    fun `pool contents - after exceed maxObjects, after exceed maxQueueSize - "receive an invalid object back"`() {
        takeAndWait(maxObjects)
        takeQueued(maxQueueSize)

        factory.reject += MyPooledObject(1)
        verifyException(ExecutionException::class.java, IllegalStateException::class.java) {
            pool.giveBack(MyPooledObject(1)).get()
        }

        await.untilCallTo { pool.usedItems.size } matches { it == maxObjects }
        assertThat(pool.waitingForItem.size).isEqualTo(maxQueueSize - 1)
        assertThat(pool.availableItems.size).isEqualTo(0)
    }

    @Test
    fun `pool contents - after exceed maxObjects, after exceed maxQueueSize - "receive maxQueueSize objects back"`() {
        takeAndWait(maxObjects)
        takeQueued(maxQueueSize)

        for (i in 1..maxObjects) {
            (pool.giveBack(MyPooledObject(1))).get()
        }

        await.untilCallTo { pool.usedItems.size } matches { it == maxObjects }
        assertThat(pool.waitingForItem.size).isEqualTo(0)
        assertThat(pool.availableItems.size).isEqualTo(0)
    }

    @Test
    fun `pool contents - after exceed maxObjects, after exceed maxQueueSize - "receive maxQueueSize invalid objects back"`() {
        takeAndWait(maxObjects)
        takeQueued(maxQueueSize)

        for (i in 1..maxObjects) {
            factory.reject += MyPooledObject(i)
            verifyException(ExecutionException::class.java, IllegalStateException::class.java) {
                pool.giveBack(MyPooledObject(i)).get()
            }
        }
        await.untilCallTo { pool.usedItems.size } matches { it == maxObjects }
        assertThat(pool.waitingForItem.size).isEqualTo(0)
        assertThat(pool.availableItems.size).isEqualTo(0)
    }

    @Test
    fun `pool contents - after exceed maxObjects, after exceed maxQueueSize - "receive maxQueueSize + 1 object back"`() {
        takeAndWait(maxObjects)
        takeQueued(maxQueueSize)

        for (i in 1..maxObjects) {
            (pool.giveBack(MyPooledObject(i))).get()
        }

        (pool.giveBack(MyPooledObject(1))).get()
        await.untilCallTo { pool.usedItems.size } matches { it == maxObjects - 1 }
        assertThat(pool.waitingForItem.size).isEqualTo(0)
        assertThat(pool.availableItems.size).isEqualTo(1)
    }

    @Test
    fun `pool contents - after exceed maxObjects, after exceed maxQueueSize - "receive maxQueueSize + 1 invalid object back"`() {
        takeAndWait(maxObjects)
        takeQueued(maxQueueSize)

        for (i in 1..maxObjects) {
            (pool.giveBack(MyPooledObject(i))).get()
        }
        await.untilCallTo { pool.usedItems.size } matches { it == maxObjects }
        await.untilCallTo { pool.waitingForItem.size } matches { it == 0 }
        await.untilCallTo { pool.availableItems.size } matches { it == 0 }

        factory.reject += MyPooledObject(1)
        verifyException(ExecutionException::class.java, IllegalStateException::class.java) {
            (pool.giveBack(MyPooledObject(1)).get())
        }
        await.untilCallTo { pool.usedItems.size } matches { it == maxObjects - 1 }
        await.untilCallTo { pool.waitingForItem.size } matches { it == 0 }
        await.untilCallTo { pool.availableItems.size } matches { it == 0 }
    }

    @Test
    fun `pool contents - after exceed maxObjects, after exceed maxQueueSize - "gives back the connection to the original pool"`() {
        val executor = Executors.newFixedThreadPool(20)

        val takes =
            (0 until 30).map { _ ->
                CompletableFuture.completedFuture(Unit).flatMapAsync(executor) { pool.take() }
            }
        val futureOfAll =
            CompletableFuture.allOf(*takes.toTypedArray())
                .mapAsync(executor) { _ -> takes.map { it.get() } }
        val takesAndReturns =
            futureOfAll.flatMapAsync(executor) { items ->
                CompletableFuture.allOf(* items.map { pool.giveBack(it) }.toTypedArray())
            }

        takesAndReturns.get()

        executor.shutdown()
        assertThat(pool.usedItems.size).isEqualTo(0)
        assertThat(pool.waitingForItem.size).isEqualTo(0)
        await.untilCallTo { pool.availableItems.size } matches { it == 30 }
    }
}

private data class MyPooledObject(val i: Int) : PooledObject {
    override val creationTime: Long get() = 1
    override val id: String get() = "$i"
}
