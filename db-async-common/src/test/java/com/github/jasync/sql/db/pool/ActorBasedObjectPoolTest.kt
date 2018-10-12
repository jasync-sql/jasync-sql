package com.github.jasync.sql.db.pool

import com.github.jasync.sql.db.util.Try
import com.github.jasync.sql.db.util.failed
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.Test
import java.util.concurrent.CompletableFuture

class ActorBasedObjectPoolTest {

  private val factory = ForTestingMyFactory()
  private val configuration = PoolConfiguration.Default.copy(maxObjects = 10, maxQueueSize = Int.MAX_VALUE)
  private var tested = ActorBasedObjectPool(factory, configuration)

  @Test(expected = PoolAlreadyTerminatedException::class)
  fun `check no take operations can be done after pool is close`() {
    tested.close().get()
    tested.take().get()
  }

  @Test
  fun `basic take operation`() {
    val result = tested.take().get()
    assertThat(result).isEqualTo(factory.created[0])
    assertThat(result).isEqualTo(factory.validated[0])
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
  }

  @Test
  fun `basic pool size 1 take2 one should not be completed until 1 returned`() {
    tested = ActorBasedObjectPool(factory, configuration.copy(
        maxObjects = 1
    ))
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
    assertThat(tested.availableItems).isEqualTo(listOf(widget))
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
    assertThat(tested.availableItems).isEqualTo(listOf(widget))
    tested.testAvailableItems()
    await.untilCallTo { factory.tested.size } matches { it == 1 }
    assertThat(tested.availableItems).isEqualTo(emptyList<ForTestingMyWidget>())
    factory.tested.getValue(widget).completeExceptionally(Exception("failed"))
    await.untilCallTo { tested.usedItems } matches { it == emptyList<ForTestingMyWidget>() }
    assertThat(tested.availableItems).isEqualTo(emptyList<ForTestingMyWidget>())
  }

  //test for configurations
}

class ForTestingMyWidget(var isOk: Boolean = true)

class ForTestingMyFactory() : AsyncObjectFactory<ForTestingMyWidget> {

  val created = mutableListOf<ForTestingMyWidget>()
  val destroyed = mutableListOf<ForTestingMyWidget>()
  val validated = mutableListOf<ForTestingMyWidget>()
  val tested = mutableMapOf<ForTestingMyWidget, CompletableFuture<ForTestingMyWidget>>()
  var failCreation: Boolean = false
  var failCreationFuture: Boolean = false
  var failValidation: Boolean = false
  var failValidationTry: Boolean = false

  override fun create(): CompletableFuture<ForTestingMyWidget> {
    if (failCreation) {
      throw Exception("failed to create")
    }
    if (failCreationFuture) {
      return CompletableFuture<ForTestingMyWidget>().failed(Exception("failed to create"))
    }
    val widget = ForTestingMyWidget()
    created += widget
    return CompletableFuture.completedFuture(widget)
  }

  override fun destroy(item: ForTestingMyWidget) {
    TODO("not implemented")
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
