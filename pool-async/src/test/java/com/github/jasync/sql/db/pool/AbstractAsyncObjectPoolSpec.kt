package com.github.jasync.sql.db.pool

import com.github.jasync.sql.db.util.Failure
import com.github.jasync.sql.db.util.Try
import com.github.jasync.sql.db.util.head
import com.github.jasync.sql.db.verifyExceptionInHierarchy
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.After
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * This spec is designed abstract to allow testing of any implementation of AsyncObjectPool, against the common
 * requirements the interface expects.
 *
 * @tparam T the AsyncObjectPool being tested.
 *
 */
abstract class AbstractAsyncObjectPoolSpec<T : AsyncObjectPool<Widget>> {

    private lateinit var pool: T

    protected abstract fun createPool(
      factory: ObjectFactory<Widget> = TestWidgetFactory(),
      conf: PoolConfiguration = PoolConfiguration(
        10,
        4,
        10)
    ): T

    @After
    fun closePool() {
        if (::pool.isInitialized) {
            pool.close().get()
        }
    }

    @Test
    fun `the variant of AsyncObjectPool should successfully retrieve and return a Widget `() {
        pool = createPool()
        val widget = pool.take().get()

        assertNotNull(widget)

        val thePool = pool.giveBack(widget).get()
        assertEquals(pool, thePool)
    }

    @Test(expected = ExecutionException::class)
    fun `the variant of AsyncObjectPool should reject Widgets that did not come from it`() {
        pool = createPool()
        pool.giveBack(Widget(TestWidgetFactory())).get()
    }

    @Test
    fun `scale contents`() {
        val factory = spyk<TestWidgetFactory>()
        pool = createPool(
                factory = factory,
                conf = PoolConfiguration(
                  maxObjects = 5,
                  maxIdle = 2,
                  maxObjectTtl = 5000,
                  maxQueueSize = 5,
                  validationInterval = 2000

                )
        )

        //"can take up to maxObjects"
        val taken: List<Widget> = (1..5).map { pool.take().get() }
        assertEquals(5, taken.size)
        (0..4).forEach {
            assertThat(taken[it]).isNotNull()
        }
        //"does not attempt to expire taken items"
        // Wait 3 seconds to ensure idle/maxConnectionTtl check has run at least once
        Thread.sleep(3000)
        verify(exactly = 0) { factory.destroy(any()) }

        //reset(factory) // Considered bad form, but necessary as we depend on previous state in these tests

        //"takes maxObjects back"
        val returns = taken.map {
            pool.giveBack(it).get()
        }
        assertEquals(5, returns.size)
        (0..4).forEach {
            assertThat(returns[it]).isEqualTo(pool)
        }

        //"protest returning an item that was already returned"
        verifyExceptionInHierarchy(IllegalStateException::class.java) {
            pool.giveBack(taken.head).get()
        }

        //"destroy down to maxIdle widgets"
        Thread.sleep(3000)
        verify(exactly = 5) { factory.destroy(any()) }
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
            assertThat(e::class.java).isEqualTo(exType)
            causeType?.let { assertThat(e.cause!!::class.java).isEqualTo(it) }
        }
    }

    @Test
    fun `queue requests after running out`() {
        pool = createPool(conf = PoolConfiguration(
          maxIdle = 4,
          maxObjects = 2,
          maxQueueSize = 1))
        val widgets = (1..2).map { pool.take().get() }
        val future = pool.take()

        // Wait five seconds
        Thread.sleep(5000)

        val failedFuture = pool.take()

        assertThat(future).isNotCompleted
        verifyException(ExecutionException::class.java, PoolExhaustedException::class.java) {
            failedFuture.get()
        }
        assertThat(pool.giveBack(widgets.head).get()).isEqualTo(pool)
        assertThat(future.get(5, TimeUnit.SECONDS)).isEqualTo(widgets.head)
    }

    @Test
    fun `refuse to allow take after being closed`() {
        pool = createPool()
        assertThat(pool.close().get()).isEqualTo(pool)
        verifyExceptionInHierarchy(PoolAlreadyTerminatedException::class.java) {
            pool.take().get()
        }
    }

    @Test
    fun `allow being closed more than once`() {
        pool = createPool()
        assertThat(pool.close().get()).isEqualTo(pool)
        assertThat(pool.close().get()).isEqualTo(pool)
    }


    @Test
    fun `destroy a failed widget`() {
        val factory = spyk<TestWidgetFactory>()
        pool = createPool(factory = factory)
        val widget = pool.take().get()
        assertThat(widget).isNotNull
        every { factory.validate(widget) } returns Failure(RuntimeException("This is a bad widget!"))
        verifyException(ExecutionException::class.java, RuntimeException::class.java) {
            pool.giveBack(widget).get()
        }
        awaitVerifyNoException { factory.destroy(widget) }
    }

    private fun awaitVerifyNoException(function: () -> Unit) {
        // make sure exception was not thrown
        await.ignoreExceptions().untilCallTo { verify { function() } } matches { it == Unit }
    }

    @Test
    fun `clean up widgets that die in the pool`() {
        val factory = spyk<TestWidgetFactory>()
        // Deliberately make it impossible to expire (nearly)
        pool = createPool(
                factory = factory,
                conf = PoolConfiguration(maxObjects = 10,
                                                                                          maxIdle = Long.MAX_VALUE,
                                                                                          maxQueueSize = 10,
                                                                                          validationInterval = 2000)
        )
        val widget = pool.take().get()
        assertThat(widget).isNotNull
        assertThat(pool.giveBack(widget).get()).isEqualTo(pool)
        verify { factory.validate(widget) }
        verify(exactly = 0) { factory.destroy(widget) }
        Thread.sleep(3000)
        verify(atLeast = 2) { factory.validate(widget) }
        every { factory.validate(widget) } returns Failure(RuntimeException("Test Exception, Not an Error"))
        Thread.sleep(3000)
        verify { factory.destroy(widget) }
        pool.take().get()
        verify(exactly = 2) { factory.create() }
    }
}

var idCounter = 0

class Widget(val factory: TestWidgetFactory, override val creationTime: Long = System.currentTimeMillis()) :
  PooledObject {
    override val id: String by lazy { "${idCounter++}" }
}

class TestWidgetFactory :
  ObjectFactory<Widget> {

    override fun create(): CompletableFuture<Widget> = CompletableFuture.completedFuture(
      Widget(this))

    override fun destroy(item: Widget) {}

    override fun validate(item: Widget): Try<Widget> = Try {
        if (item.factory == this)
            item
        else
            throw IllegalArgumentException("Not our item")
    }

}


