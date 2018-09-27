package com.github.jasync.sql.db.pool

import com.github.jasync.sql.db.util.Try
import com.github.jasync.sql.db.util.flatMap
import com.github.jasync.sql.db.util.map
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class PartitionedAsyncObjectPoolSpec {

  val config = PoolConfiguration(100, Long.MAX_VALUE, 100, 5000)


  class ForTestingObjectFactory : ObjectFactory<Int> {
    val reject = HashSet<Int>()
    var failCreate = false
    val current = AtomicInteger(0)

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
        throw IllegalStateException()
      }
      return Try.just(item)
    }
  }

  val factory = ForTestingObjectFactory()


  val pool = PartitionedAsyncObjectPool(factory, config, 2)
  val maxObjects = config.maxObjects / 2
  //val maxIdle = config.maxIdle / 2
  val maxQueueSize = config.maxQueueSize / 2

  private fun takeAndWait(objects: Int) {
    for (it in 1..objects) {
      pool.take().get()
    }
  }

  private fun takeQueued(objects: Int) {
    takeNoWait(objects)
    await.untilCallTo { pool.queued().size } matches { it == objects }
  }
  private fun takeNoWait(objects: Int) {
    for (it in 1..objects) {
      pool.take()
    }
  }


  @Test
  fun `pool contents - before exceed maxObjects - take one element`() {
    takeAndWait(1)
    assertThat(pool.inUse().size).isEqualTo(1)
    assertThat(pool.queued().size).isEqualTo(0)
    assertThat(pool.availables().size).isEqualTo(0)
  }

  private fun verifyException(exType: Class<out java.lang.Exception>,
                              causeType: Class<out java.lang.Exception>? = null,
                              body: () -> Unit) {
    try {
      body()
      throw Exception("${exType.simpleName}->${causeType?.simpleName} was not thrown")
    } catch (e: Exception) {
      assertThat(e::class.java).isEqualTo(exType)
      causeType?.let { assertThat(e.cause!!::class.java).isEqualTo(it) }
    }
  }

  @Test
  fun `pool contents - before exceed maxObjects - take one element and return it invalid`() {
    takeAndWait(1)
    factory.reject += 1

    verifyException(ExecutionException::class.java, IllegalStateException::class.java) {
      pool.giveBack(1).get()
    }

    assertThat(pool.inUse().size).isEqualTo(0)
    assertThat(pool.queued().size).isEqualTo(0)
    assertThat(pool.availables().size).isEqualTo(0)
  }

  @Test
  fun `pool contents - before exceed maxObjects - take one failed element`() {
    factory.failCreate = true
    verifyException(ExecutionException::class.java, IllegalStateException::class.java) {
      takeAndWait(1)
    }
    assertThat(pool.inUse().size).isEqualTo(0)
    assertThat(pool.queued().size).isEqualTo(0)
    assertThat(pool.availables().size).isEqualTo(0)
  }

  @Test
  fun `pool contents - before exceed maxObjects - take maxObjects`() {
    takeAndWait(maxObjects)

    assertThat(pool.inUse().size).isEqualTo(maxObjects)
    assertThat(pool.queued().size).isEqualTo(0)
    assertThat(pool.availables().size).isEqualTo(0)
  }

  @Test
  fun `pool contents - before exceed maxObjects - take maxObjects - 1 and take one failed`() {
    takeAndWait(maxObjects - 1)

    factory.failCreate = true
    verifyException(ExecutionException::class.java, IllegalStateException::class.java) {
      takeAndWait(1)
    }
    assertThat(pool.inUse().size).isEqualTo(maxObjects - 1)
    assertThat(pool.queued().size).isEqualTo(0)
    assertThat(pool.availables().size).isEqualTo(0)
  }

  @Test
  fun `pool contents - before exceed maxObjects - "take maxObjects and receive one back"`() {
    takeAndWait(maxObjects)
    pool.giveBack(1).get()

    assertThat(pool.inUse().size).isEqualTo(maxObjects - 1)
    assertThat(pool.queued().size).isEqualTo(0)
    assertThat(pool.availables().size).isEqualTo(1)
  }

  @Test
  fun `pool contents - before exceed maxObjects - "take maxObjects and receive one invalid back"`() {
    takeAndWait(maxObjects)
    factory.reject += 1
    verifyException(ExecutionException::class.java, IllegalStateException::class.java) {
      pool.giveBack(1).get()
    }
    assertThat(pool.inUse().size).isEqualTo(maxObjects - 1)
    assertThat(pool.queued().size).isEqualTo(0)
    assertThat(pool.availables().size).isEqualTo(0)
  }


  @Test
  fun `pool contents - after exceed maxObjects, before exceed maxQueueSize - "one take queued"`() {
    takeAndWait(maxObjects)
    takeQueued(1)

    assertThat(pool.inUse().size).isEqualTo(maxObjects)
    assertThat(pool.queued().size).isEqualTo(1)
    assertThat(pool.availables().size).isEqualTo(0)
  }


  @Test
  fun `pool contents - after exceed maxObjects, before exceed maxQueueSize - "one take queued and receive one item back"`() {
    takeAndWait(maxObjects)

    val taking = pool.take()

    pool.giveBack(1).get()

    assertThat(taking.get()).isEqualTo(1)
    await.untilCallTo { pool.inUse().size } matches { it == maxObjects }
    assertThat(pool.queued().size).isEqualTo(0)
    assertThat(pool.availables().size).isEqualTo(0)
  }

  @Test
  fun `pool contents - after exceed maxObjects, before exceed maxQueueSize - "one take queued and receive one invalid item back"`() {
    takeAndWait(maxObjects)

    pool.take()
    factory.reject += 1
    verifyException(ExecutionException::class.java, IllegalStateException::class.java) {
      pool.giveBack(1).get()
    }

    assertThat(pool.inUse().size).isEqualTo(maxObjects - 1)
    assertThat(pool.queued().size).isEqualTo(1)
    assertThat(pool.availables().size).isEqualTo(0)
  }

  @Test
  fun `pool contents - after exceed maxObjects, before exceed maxQueueSize - "maxQueueSize takes queued"`() {
    takeAndWait(maxObjects)
    takeQueued(maxQueueSize)

    assertThat(pool.inUse().size).isEqualTo(maxObjects)
    assertThat(pool.queued().size).isEqualTo(maxQueueSize)
    assertThat(pool.availables().size).isEqualTo(0)
  }


  @Test
  fun `pool contents - after exceed maxObjects, before exceed maxQueueSize - "maxQueueSize takes queued and receive one back"`() {
    takeAndWait(maxObjects)

    val taking = pool.take()
    takeNoWait(maxQueueSize - 1)

    pool.giveBack(10).get()

    assertThat((taking).get()).isEqualTo(10)
    await.untilCallTo { pool.inUse().size } matches { it == maxObjects }
    assertThat(pool.queued().size).isEqualTo(maxQueueSize - 1)
    assertThat(pool.availables().size).isEqualTo(0)
  }

  @Test
  fun `pool contents - after exceed maxObjects, before exceed maxQueueSize - "maxQueueSize takes queued and receive one invalid back"`() {
    takeAndWait(maxObjects)
    takeQueued(maxQueueSize)

    factory.reject += 11
    verifyException(ExecutionException::class.java, IllegalStateException::class.java) {
      pool.giveBack(11).get()
    }

    assertThat(pool.inUse().size).isEqualTo(maxObjects - 1)
    assertThat(pool.queued().size).isEqualTo(maxQueueSize)
    assertThat(pool.availables().size).isEqualTo(0)

  }

  @Test
  fun `pool contents - after exceed maxObjects, after exceed maxQueueSize - "start to reject takes"`() {
    takeAndWait(maxObjects)
    takeQueued(maxQueueSize)

    verifyException(ExecutionException::class.java, PoolExhaustedException::class.java) {
      (pool.take().get())
    }

    assertThat(pool.inUse().size).isEqualTo(maxObjects)
    assertThat(pool.queued().size).isEqualTo(maxQueueSize)
    assertThat(pool.availables().size).isEqualTo(0)
  }

  @Test
  fun `pool contents - after exceed maxObjects, after exceed maxQueueSize - "receive an object back"`() {
    takeAndWait(maxObjects)
    takeQueued(maxQueueSize)


    (pool.giveBack(1)).get()

    await.untilCallTo { pool.inUse().size } matches { it == maxObjects }
    assertThat(pool.queued().size).isEqualTo(maxQueueSize - 1)
    assertThat(pool.availables().size).isEqualTo(0)
  }

  @Test
  fun `pool contents - after exceed maxObjects, after exceed maxQueueSize - "receive an invalid object back"`() {
    takeAndWait(maxObjects)
    takeQueued(maxQueueSize)


    factory.reject += 1
    verifyException(ExecutionException::class.java, IllegalStateException::class.java) {
      pool.giveBack(1).get()
    }

    assertThat(pool.inUse().size).isEqualTo(maxObjects - 1)
    assertThat(pool.queued().size).isEqualTo(maxQueueSize)
    assertThat(pool.availables().size).isEqualTo(0)
  }

  @Test
  fun `pool contents - after exceed maxObjects, after exceed maxQueueSize - "receive maxQueueSize objects back"`() {
    takeAndWait(maxObjects)
    takeQueued(maxQueueSize)


    for (i in 1..maxQueueSize) {
      (pool.giveBack(i)).get()
    }

    assertThat(pool.inUse().size).isEqualTo(maxObjects)
    assertThat(pool.queued().size).isEqualTo(0)
    assertThat(pool.availables().size).isEqualTo(0)
  }

  @Test
  fun `pool contents - after exceed maxObjects, after exceed maxQueueSize - "receive maxQueueSize invalid objects back"`() {
    takeAndWait(maxObjects)
    takeQueued(maxQueueSize)


    for (i in 1..maxQueueSize) {
      factory.reject += i
      verifyException(ExecutionException::class.java, IllegalStateException::class.java) {
        pool.giveBack(i).get()
      }
    }
    await.untilCallTo { pool.inUse().size } matches { it == maxObjects - maxQueueSize }
    assertThat(pool.queued().size).isEqualTo(maxQueueSize)
    assertThat(pool.availables().size).isEqualTo(0)
  }

  @Test
  fun `pool contents - after exceed maxObjects, after exceed maxQueueSize - "receive maxQueueSize + 1 object back"`() {
    takeAndWait(maxObjects)
    takeQueued(maxQueueSize)


    for (i in 1..maxQueueSize) {
      (pool.giveBack(i)).get()
    }

    (pool.giveBack(1)).get()
    assertThat(pool.inUse().size).isEqualTo(maxObjects - 1)
    assertThat(pool.queued().size).isEqualTo(0)
    assertThat(pool.availables().size).isEqualTo(1)
  }

  @Test
  fun `pool contents - after exceed maxObjects, after exceed maxQueueSize - "receive maxQueueSize + 1 invalid object back"`() {
    takeAndWait(maxObjects)
    takeQueued(maxQueueSize)


    for (i in 1..maxQueueSize) {
      (pool.giveBack(i)).get()
    }

    factory.reject += 1
    verifyException(ExecutionException::class.java, IllegalStateException::class.java) {
      (pool.giveBack(1).get())
    }
    assertThat(pool.inUse().size).isEqualTo(maxObjects - 1)
    assertThat(pool.queued().size).isEqualTo(0)
    assertThat(pool.availables().size).isEqualTo(0)
  }

  @Test
  fun `pool contents - after exceed maxObjects, after exceed maxQueueSize - "gives back the connection to the original pool"`() {
    val executor = Executors.newFixedThreadPool(20)

    val takes =
        (0 until 30).map { _ ->
          CompletableFuture.completedFuture(Unit).flatMap(executor) { pool.take() }
        }
    val futureOfAll = CompletableFuture.allOf(*takes.toTypedArray()).map(executor) { _ -> takes.map { it.get() } }
    val takesAndReturns =
        futureOfAll.flatMap(executor) { items ->
          CompletableFuture.allOf(* items.map { pool.giveBack(it) }.toTypedArray())
        }

    takesAndReturns.get()

    executor.shutdown()
    assertThat(pool.inUse().size).isEqualTo(0)
    assertThat(pool.queued().size).isEqualTo(0)
    assertThat(pool.availables().size).isEqualTo(30)
  }

}



