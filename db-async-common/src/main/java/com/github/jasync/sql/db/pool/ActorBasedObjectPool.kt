package com.github.jasync.sql.db.pool

import com.github.jasync.sql.db.util.Failure
import com.github.jasync.sql.db.util.Success
import com.github.jasync.sql.db.util.Try
import com.github.jasync.sql.db.util.XXX
import com.github.jasync.sql.db.util.onComplete
import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.channels.actor
import mu.KotlinLogging
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.CompletableFuture

private val logger = KotlinLogging.logger {}

class ActorBasedObjectPool<T : Any>(objectFactory: AsyncObjectFactory<T>, configuration: PoolConfiguration) : AsyncObjectPool2<T> {
  var closed = false

  override fun take(): CompletableFuture<T> {
    if (closed) {
      throw PoolAlreadyTerminatedException()
    }
    val future = CompletableFuture<T>()
    val offered = actor.offer(Take(future))
    if (!offered) {
      future.completeExceptionally(Exception("could not offer to actor"))
    }
    return future
  }

  override fun giveBack(item: T): CompletableFuture<Unit> {
    val future = CompletableFuture<Unit>()
    val offered = actor.offer(GiveBack(item, future))
    if (!offered) {
      future.completeExceptionally(Exception("could not offer to actor"))
    }
    return future
  }

  override fun close(): CompletableFuture<Unit> {
    closed = true
    //TODO actor.close()
    return CompletableFuture.completedFuture(Unit)
  }

  fun testAvailableItems() {
    val offered = actor.offer(TestAvailableItems())
    if (!offered) {
      logger.warn { "failed to offer to actor - testAvailableItems()" }
    }
  }

  private val actorInstance = ObjectPoolActor(objectFactory, configuration, { actor })
  private val actor: SendChannel<ActorObjectPoolMessage<T>> = GlobalScope.actor(
      context = Dispatchers.Default,
      capacity = Int.MAX_VALUE,
      start = CoroutineStart.DEFAULT,
      onCompletion = null) {
    for (message in channel) {
      actorInstance.onReceive(message)
    }
  }
  val availableItems: List<T> get() = actorInstance.availableItemsList
  val usedItems: List<T> get() = actorInstance.usedItemsList
}

private sealed class ActorObjectPoolMessage<T>
private class Take<T : Any>(val future: CompletableFuture<T>) : ActorObjectPoolMessage<T>()
private class GiveBack<T : Any>(val returnedItem: T, val future: CompletableFuture<Unit>, val exception: Throwable? = null) : ActorObjectPoolMessage<T>()
private class Created<T : Any>(val itemId: Int, val item: Try<T>, val takeAskFuture: CompletableFuture<T>) : ActorObjectPoolMessage<T>()
private class TestAvailableItems<T : Any> : ActorObjectPoolMessage<T>()

private class ObjectPoolActor<T : Any>(private val objectFactory: AsyncObjectFactory<T>,
                                       private val configuration: PoolConfiguration,
                                       private val channelProvider: () -> SendChannel<ActorObjectPoolMessage<T>>) {

  private val availableItems: Queue<T> = LinkedList<T>()
  private val waitingQueue: Queue<CompletableFuture<T>> = LinkedList<CompletableFuture<T>>()
  private val inUseItems = mutableSetOf<T>()
  private val inCreateItems = mutableMapOf<Int, CompletableFuture<T>>()
  private var itemIndex = 0
  private val channel: SendChannel<ActorObjectPoolMessage<T>> by lazy { channelProvider() }

  val availableItemsList: List<T> get() = availableItems.toList()
  val usedItemsList: List<T> get() = inUseItems.toList()


  fun onReceive(message: ActorObjectPoolMessage<T>) {
    logger.trace { "received message $message" }
    when (message) {
      is Take<T> -> handleTake(message)
      is GiveBack<T> -> handleGiveBack(message)
      is Created<T> -> handleCreated(message)
      is TestAvailableItems<T> -> handleTestAvailableItems()
      else -> XXX("no handle for message $message")
    }
  }

  private fun handleTestAvailableItems() {
    //TODO check last used
    availableItems.forEach {
      val test = objectFactory.test(it)
      inUseItems.add(it)
      //TODO check whenComplete exception handling
      test.whenComplete { _, t ->
        offerOrLog(GiveBack(it, CompletableFuture(), t)) { "test item" }
      }
    }
    availableItems.clear()
  }

  private fun offerOrLog(message: ActorObjectPoolMessage<T>, logMessage: () -> String) {
    val offered = channel.offer(message)
    if (!offered) {
      logger.warn { "failed to offer on ${logMessage()}" }
    }
  }

  private fun handleCreated(message: Created<T>) {
    //TODO we can think about optimization that when an item returned give it to someone waiting for created
    val removed = inCreateItems.remove(message.itemId)
    if (removed == null) {
      logger.warn { "could not find connection ${message.itemId}" }
    }
    val future = message.takeAskFuture
    when (message.item) {
      is Failure -> future.completeExceptionally(message.item.exception)
      is Success -> {
        try {
          message.item.value.borrowTo(future)
        } catch (e: Exception) {
          future.completeExceptionally(e)
        }
      }
    }
  }

  private fun T.borrowTo(future: CompletableFuture<T>, validate: Boolean = true) {
    if (validate) {
      validate(this)
    }
    inUseItems.add(this)
    future.complete(this)
  }

  private fun handleGiveBack(message: GiveBack<T>) {
    try {
      inUseItems.remove(message.returnedItem)
      if (message.exception != null) {
        throw message.exception
      }
      validate(message.returnedItem)
      message.future.complete(Unit)
      if (waitingQueue.isEmpty()) {
        availableItems.add(message.returnedItem)
      } else {
        val waitingFuture = waitingQueue.remove()
        message.returnedItem.borrowTo(waitingFuture, validate = false)
      }
    } catch (e: Exception) {
      message.future.completeExceptionally(e)
    }
  }

  private fun handleTake(message: Take<T>) {
    //take from available
    while (availableItems.isNotEmpty()) {
      try {
        val item = availableItems.remove()
        item.borrowTo(message.future)
        return
      } catch (e: Exception) {
        logger.debug { "validation of object failed, removing it from pool: ${e.message}" }
      }
    }
    //create new or wait (available is empty)
    try {
      if (inUseItems.size < configuration.maxObjects) {
        createObject(message)
      } else {
        waitingQueue.add(message.future)
      }
    } catch (e: Exception) {
      message.future.completeExceptionally(e)
    }
  }

  private fun createObject(message: Take<T>) {
    val created = objectFactory.create()
    val itemId = itemIndex
    itemIndex++
    inCreateItems[itemId] = created
    created.onComplete { tried ->
      offerOrLog(Created(itemId, tried, message.future)) {
        "failed to offer on created item $itemId"
      }
    }
  }

  private fun validate(a: T) {
    val tried = objectFactory.validate(a)
    when (tried) {
      is Failure -> throw tried.exception
    }
  }
}
