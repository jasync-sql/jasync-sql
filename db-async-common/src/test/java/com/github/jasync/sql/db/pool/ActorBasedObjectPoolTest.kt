package com.github.jasync.sql.db.pool

import com.github.jasync.sql.db.util.FP
import com.github.jasync.sql.db.util.Try
import com.github.jasync.sql.db.util.isSuccess
import com.github.jasync.sql.db.verifyException
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

class ActorBasedObjectPoolTest {

    private val factory = ForTestingMyFactory()
    private val configuration = PoolConfiguration(
        maxObjects = 10, maxQueueSize = Int.MAX_VALUE,
        validationInterval = Long.MAX_VALUE, maxIdle = Long.MAX_VALUE,
        maxObjectTtl = null
    )
    private var tested = ActorBasedObjectPool(factory, configuration, testItemsPeriodically = false)

    @Test
    fun `check no take operations can be done after pool is close and connection is cleanup`() {
        val widget = tested.take().get()
        tested.close().get()
        verifyException(PoolAlreadyTerminatedException::class.java) {
            tested.take().get()
        }
        assertThat(factory.destroyed).isEqualTo(listOf(widget))
    }

    @Test
    fun `basic take operation`() {
        val result = tested.take().get()
        assertThat(result).isEqualTo(factory.created[0])
        assertThat(result).isEqualTo(factory.validated[0])
    }

    @Test
    fun `basic take operation - when create is stuck should be timeout`() {
        tested = ActorBasedObjectPool(
            factory, configuration.copy(
                createTimeout = 10
            ), false
        )
        factory.creationStuck = true
        val result = tested.take()
        Thread.sleep(20)
        tested.testAvailableItems()
        await.untilCallTo { result.isCompletedExceptionally } matches { it == true }
    }

    @Test
    fun `basic take operation - when create is little stuck should not be timeout (create timeout is 5 sec)`() {
        tested = ActorBasedObjectPool(
            factory, configuration.copy(
                createTimeout = 5000
            ), false
        )
        factory.creationStuckTime = 10
        val result = tested.take()
        Thread.sleep(20)
        tested.testAvailableItems()
        await.untilCallTo { result.isSuccess } matches { it == true }
    }

    @Test
    fun `check items periodically`() {
        tested = ActorBasedObjectPool(
            factory, configuration.copy(
                validationInterval = 1000
            ),
            testItemsPeriodically = true
        )
        val result = tested.take().get()
        tested.giveBack(result)
        Thread.sleep(1000)
        await.untilCallTo { factory.tested } matches { it?.containsKey(result) == true }
    }

    @Test(expected = Exception::class)
    fun `basic take operation when create failed future should fail`() {
        factory.failCreation = true
        tested.take().get()
    }

    @Test(expected = Exception::class)
    fun `basic take operation when create failed future should fail 2`() {
        factory.failCreationFuture = true
        tested.take().get()
    }

    @Test(expected = Exception::class)
    fun `basic take operation when validation failed future should fail`() {
        factory.failValidation = true
        tested.take().get()
    }

    @Test(expected = Exception::class)
    fun `basic take operation when validation failed future should fail 2`() {
        factory.failValidationTry = true
        tested.take().get()
    }

    @Test
    fun `basic take-return-take operation`() {
        val result = tested.take().get()
        tested.giveBack(result).get()
        val result2 = tested.take().get()
        assertThat(result).isEqualTo(result2)
        assertThat(factory.validated).isEqualTo(listOf(result, result, result))
    }

    @Test
    fun `take2-return2-take first not validated second is ok should be returned`() {
        val result = tested.take().get()
        val result2 = tested.take().get()
        tested.giveBack(result).get()
        tested.giveBack(result2).get()
        result.isOk = false
        val result3 = tested.take().get()
        assertThat(result3).isEqualTo(result2)
        assertThat(factory.destroyed).isEqualTo(listOf(result))
    }

    @Test
    fun `basic pool size 1 take2 one should not be completed until 1 returned`() {
        tested = ActorBasedObjectPool(
            factory, configuration.copy(
                maxObjects = 1
            ), false
        )
        val result = tested.take().get()
        val result2Future = tested.take()
        assertThat(result2Future).isNotCompleted
        tested.giveBack(result).get()
        result2Future.get()
    }

    @Test
    fun `basic pool item validation should return to pool after test`() {
        val widget = tested.take().get()
        tested.giveBack(widget).get()
        await.untilCallTo { tested.availableItems } matches { it == listOf(widget) }
        tested.testAvailableItems()
        await.untilCallTo { factory.tested.size } matches { it == 1 }
        assertThat(tested.availableItems).isEqualTo(emptyList<ForTestingMyWidget>())
        factory.tested.getValue(widget).complete(widget)
        await.untilCallTo { tested.availableItems } matches { it == listOf(widget) }
    }

    @Test
    fun `basic pool item validation should not return to pool after failed test`() {
        val widget = tested.take().get()
        tested.giveBack(widget).get()
        await.untilCallTo { tested.availableItems } matches { it == listOf(widget) }
        tested.testAvailableItems()
        await.untilCallTo { factory.tested.size } matches { it == 1 }
        assertThat(tested.availableItems).isEqualTo(emptyList<ForTestingMyWidget>())
        factory.tested.getValue(widget).completeExceptionally(Exception("failed"))
        await.untilCallTo { tested.usedItems } matches { it == emptyList<ForTestingMyWidget>() }
        assertThat(tested.availableItems).isEqualTo(emptyList<ForTestingMyWidget>())
        assertThat(factory.destroyed).isEqualTo(listOf(widget))
    }

    @Test
    fun `on test items pool should reclaim idle items`() {
        tested = ActorBasedObjectPool(
            factory, configuration.copy(
                maxIdle = 10
            ), false
        )
        val widget = tested.take().get()
        tested.giveBack(widget).get()
        Thread.sleep(20)
        tested.testAvailableItems()
        await.untilCallTo { factory.destroyed } matches { it == listOf(widget) }
        assertThat(tested.availableItems).isEmpty()
    }

    @Test
    fun `on take items pool should reclaim items pass ttl`() {
        tested = ActorBasedObjectPool(factory, configuration.copy(maxObjectTtl = 50), false)
        val widget = tested.take().get()
        Thread.sleep(70)
        tested.giveBack(widget).get()
        val widget2 = tested.take().get()
        assertThat(widget).isNotEqualTo(widget2)
        assertThat(factory.created.size).isEqualTo(2)
        assertThat(factory.destroyed[0]).isEqualTo(widget)
    }

    @Test
    fun `on test items pool should reclaim aged-out items`() {
        tested = ActorBasedObjectPool(factory, configuration.copy(maxObjectTtl = 50), false)
        val widget = tested.take().get()
        tested.giveBack(widget).get()
        Thread.sleep(70)
        tested.testAvailableItems()
        await.untilCallTo { factory.destroyed } matches { it == listOf(widget) }
        assertThat(tested.availableItems).isEmpty()
    }

    @Test
    fun `on test of item that last test timeout pool should destroy item`() {
        tested = ActorBasedObjectPool(
            factory, configuration.copy(
                testTimeout = 10
            ), false
        )
        val widget = tested.take().get()
        tested.giveBack(widget).get()
        tested.testAvailableItems()
        Thread.sleep(20)
        tested.testAvailableItems()
        await.untilCallTo { factory.destroyed } matches { it == listOf(widget) }
        assertThat(tested.availableItems).isEmpty()
        assertThat(tested.usedItems).isEmpty()
    }

    @Test
    fun `on query timeout pool should destroy item`() {
        tested = ActorBasedObjectPool(
            factory, configuration.copy(
                queryTimeout = 10
            ), false,
            extraTimeForTimeoutCompletion = 1
        )
        val widget = tested.take().get()
        Thread.sleep(20)
        tested.testAvailableItems()
        await.untilCallTo { factory.destroyed } matches { it == listOf(widget) }
        assertThat(tested.availableItems).isEmpty()
        assertThat(tested.usedItems).isEmpty()
    }

    @Test
    fun `when queue is bigger then max waiting, future should fail`() {
        tested = ActorBasedObjectPool(
            factory, configuration.copy(
                maxObjects = 1,
                maxQueueSize = 1
            ), false
        )
        tested.take().get()
        tested.take()
        verifyException(ExecutionException::class.java, PoolExhaustedException::class.java) {
            tested.take().get()
        }
    }

    @Test
    fun `test for leaks detection - we are taking a widget but "lost" it so it should be cleaned up`() {
        tested = ActorBasedObjectPool(
            ForTestingWeakMyFactory(), configuration.copy(
                maxObjects = 1,
                maxQueueSize = 1
            ), false
        )
        takeLostItem()
        Thread.sleep(1000)
        System.gc()
        await.untilCallTo { tested.usedItemsSize } matches { it == 0 }
        await.untilCallTo { tested.waitingForItemSize } matches { it == 0 }
        await.untilCallTo { tested.availableItemsSize } matches { it == 0 }
        System.gc() //to show leak in logging
        Thread.sleep(1000)
    }

    private fun takeLostItem() {
        tested.take().get()
    }

}

private var widgetId = 0

class ForTestingMyWidget(var isOk: Boolean = true, override val creationTime: Long = System.currentTimeMillis()) :
    PooledObject {
    override val id: String by lazy { (widgetId++).toString() }
}

class ForTestingWeakMyFactory : ObjectFactory<ForTestingMyWidget> {
    override fun create(): CompletableFuture<out ForTestingMyWidget> {
        val widget = ForTestingMyWidget()
        return CompletableFuture.completedFuture(widget)
    }

    override fun destroy(item: ForTestingMyWidget) {
    }

    override fun validate(item: ForTestingMyWidget): Try<ForTestingMyWidget> {
        return Try.just(item)
    }

}

class ForTestingMyFactory : ObjectFactory<ForTestingMyWidget> {

    val created = mutableListOf<ForTestingMyWidget>()
    val destroyed = mutableListOf<ForTestingMyWidget>()
    val validated = mutableListOf<ForTestingMyWidget>()
    val tested = mutableMapOf<ForTestingMyWidget, CompletableFuture<ForTestingMyWidget>>()
    var creationStuck: Boolean = false
    var creationStuckTime: Long? = null
    var failCreation: Boolean = false
    var failCreationFuture: Boolean = false
    var failValidation: Boolean = false
    var failValidationTry: Boolean = false

    override fun create(): CompletableFuture<ForTestingMyWidget> {
        if (creationStuck) {
            return CompletableFuture()
        }
        if (creationStuckTime != null) {
            val f = CompletableFuture<ForTestingMyWidget>()
            Thread{
                Thread.sleep(creationStuckTime!!)
                val widget = ForTestingMyWidget()
                created += widget
                f.complete(widget)
            }.start()
            return f
        }
        if (failCreation) {
            throw Exception("failed to create")
        }
        if (failCreationFuture) {
            return FP.failed(Exception("failed to create"))
        }
        val widget = ForTestingMyWidget()
        created += widget
        return CompletableFuture.completedFuture(widget)
    }

    override fun destroy(item: ForTestingMyWidget) {
        destroyed += item
    }

    override fun validate(item: ForTestingMyWidget): Try<ForTestingMyWidget> {
        if (failValidation) {
            throw Exception("failed to validate")
        }
        if (failValidationTry || !item.isOk) {
            return Try.raise(Exception("failed to create"))
        }
        validated += item
        return Try.just(item)
    }

    override fun test(item: ForTestingMyWidget): CompletableFuture<ForTestingMyWidget> {
        val completableFuture = CompletableFuture<ForTestingMyWidget>()
        tested += item to completableFuture
        return completableFuture
    }

}

