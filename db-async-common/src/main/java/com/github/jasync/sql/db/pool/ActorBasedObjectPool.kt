package com.github.jasync.sql.db.pool

import com.github.jasync.sql.db.util.Failure
import com.github.jasync.sql.db.util.Success
import com.github.jasync.sql.db.util.Try
import com.github.jasync.sql.db.util.XXX
import com.github.jasync.sql.db.util.mapTry
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
import java.util.concurrent.TimeoutException

private val logger = KotlinLogging.logger {}

interface PoolObject {
  val id: String
}

class ActorBasedObjectPool<T : PoolObject>(objectFactory: AsyncObjectFactory<T>, configuration: PoolConfiguration) : AsyncObjectPool2<T> {
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
    val future = CompletableFuture<Unit>()
    val offered = actor.offer(Close(future))
    if (!offered) {
      future.completeExceptionally(Exception("could not offer to actor"))
    }
    return future
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

@Suppress("unused")
private sealed class ActorObjectPoolMessage<T : PoolObject> {
  override fun toString(): String {
    return "${javaClass.simpleName} @${hashCode()}"
  }
}

private class Take<T : PoolObject>(val future: CompletableFuture<T>) : ActorObjectPoolMessage<T>()
private class GiveBack<T : PoolObject>(val returnedItem: T, val future: CompletableFuture<Unit>, val exception: Throwable? = null) : ActorObjectPoolMessage<T>() {
  override fun toString(): String {
    return "GiveBack: ${returnedItem.id} hasError=" +
        if (exception != null)
          "${exception.javaClass.simpleName} - ${exception.message}"
        else "false"
  }
}

private class Created<T : PoolObject>(val itemId: Int, val item: Try<T>, val takeAskFuture: CompletableFuture<T>) : ActorObjectPoolMessage<T>() {
  override fun toString(): String {
    val id = when (item) {
      is Success<T> -> item.value.id
      else -> "failed"
    }
    return "Created: createRequest=$itemId -> object=$id"
  }
}

private class TestAvailableItems<T : PoolObject> : ActorObjectPoolMessage<T>()
private class Close<T : PoolObject>(val future: CompletableFuture<Unit>) : ActorObjectPoolMessage<T>()

@Suppress("REDUNDANT_ELSE_IN_WHEN")
private class ObjectPoolActor<T : PoolObject>(private val objectFactory: AsyncObjectFactory<T>,
                                              private val configuration: PoolConfiguration,
                                              private val channelProvider: () -> SendChannel<ActorObjectPoolMessage<T>>) {

  private val availableItems: Queue<PoolObjectHolder<T>> = LinkedList<PoolObjectHolder<T>>()
  private val waitingQueue: Queue<CompletableFuture<T>> = LinkedList<CompletableFuture<T>>()
  private val inUseItems = mutableSetOf<ItemInUseHolder<T>>()
  private val inCreateItems = mutableMapOf<Int, ObjectHolder<CompletableFuture<T>>>()
  private var itemIndex = 0
  private val channel: SendChannel<ActorObjectPoolMessage<T>> by lazy { channelProvider() }

  val availableItemsList: List<T> get() = availableItems.map { it.item }
  val usedItemsList: List<T> get() = inUseItems.map { it.item }
  var closed = false


  fun onReceive(message: ActorObjectPoolMessage<T>) {
    logger.trace { "received message: $message" }
    when (message) {
      is Take<T> -> handleTake(message)
      is GiveBack<T> -> handleGiveBack(message)
      is Created<T> -> handleCreated(message)
      is TestAvailableItems<T> -> handleTestAvailableItems()
      is Close<T> -> handleClose(message)
      else -> XXX("no handle for message $message")
    }
  }

  private fun handleClose(message: Close<T>) {
    try {
      closed = true
      channel.close()
      availableItems.forEach { objectFactory.destroy(it.item) }
      availableItems.clear()
      inUseItems.forEach { objectFactory.destroy(it.item) }
      inUseItems.clear()
      waitingQueue.forEach { it.completeExceptionally(PoolAlreadyTerminatedException()) }
      waitingQueue.clear()
      inCreateItems.values.forEach { it.item.completeExceptionally(PoolAlreadyTerminatedException()) }
      inCreateItems.clear()
      message.future.complete(Unit)
    } catch (e: Exception) {
      message.future.completeExceptionally(e)
    }
  }

  private fun handleTestAvailableItems() {
    sendAvailableItemsToTest()
    checkItemsInCreationForTimeout()
    checkItemsInTestForTimeout()
  }

  private fun checkItemsInTestForTimeout() {
    inUseItems.removeAll {
      if (it.isInTest && it.timeElapsed > configuration.testTimeout) {
        logger.trace { "failed to test item ${it.item.id} after ${it.timeElapsed} ms" }
        it.testFuture!!.completeExceptionally(TimeoutException("failed to test item ${it.item.id} after ${it.timeElapsed} ms"))
      }
      it.isInTest && it.timeElapsed > configuration.testTimeout
    }
  }

  private fun checkItemsInCreationForTimeout() {
    inCreateItems.entries.removeAll {
      if (it.value.timeElapsed > configuration.createTimeout) {
        logger.trace { "failed to create item ${it.key} after ${it.value.timeElapsed} ms" }
      }
      it.value.item.completeExceptionally(TimeoutException("failed to create item ${it.key} after ${it.value.timeElapsed} ms"))
      it.value.timeElapsed > configuration.createTimeout
    }
  }

  private fun sendAvailableItemsToTest() {
    availableItems.forEach {
      val item = it.item
      logger.trace { "test: ${item.id} available ${it.timeElapsed} ms" }
      if (it.timeElapsed > configuration.maxIdle) {
        logger.trace { "releasing idle item ${item.id}" }
        objectFactory.destroy(item)
      } else {
        val test = objectFactory.test(item)
        inUseItems.add(ItemInUseHolder(item, isInTest = true, testFuture = test))
        test.mapTry { _, t ->
          offerOrLog(GiveBack(item, CompletableFuture(), t)) { "test item" }
        }
      }
      availableItems.clear()
    }
  }

  private fun offerOrLog(message: ActorObjectPoolMessage<T>, logMessage: () -> String) {
    val offered = channel.offer(message)
    if (!offered) {
      logger.warn { "failed to offer on ${logMessage()}" }
    }
  }

  private fun handleCreated(message: Created<T>) {
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
    logger.trace { "borrow: ${this.id}, waiting queue size is ${waitingQueue.size}" }
    inUseItems.add(ItemInUseHolder(this, isInTest = false))
    future.complete(this)
  }

  private fun handleGiveBack(message: GiveBack<T>) {
    try {
      inUseItems.removeIf { it.itemInUse === message.returnedItem }
      if (message.exception != null) {
        objectFactory.destroy(message.returnedItem)
        logger.trace { "GiveBack got exception, so destroying item ${message.returnedItem.id}, exception is ${message.exception.javaClass.simpleName} - ${message.exception.message}" }
        throw message.exception
      }
      validate(message.returnedItem)
      message.future.complete(Unit)
      if (waitingQueue.isEmpty()) {
        availableItems.add(PoolObjectHolder(message.returnedItem))
        logger.trace { "add ${message.returnedItem.id} to available items, size is ${availableItems.size}" }
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
      val itemHolder = availableItems.remove()
      try {
        itemHolder.item.borrowTo(message.future)
        return
      } catch (e: Exception) {
        logger.debug { "validation of object '${itemHolder.item.id}' failed, removing it from pool: ${e.message}" }
        objectFactory.destroy(itemHolder.item)
      }
    }
    // available is empty
    createNewItemPutInWaitQueue(message)
  }

  private fun createNewItemPutInWaitQueue(message: Take<T>) {
    try {
      if (inUseItems.size < configuration.maxObjects) {
        createObject(message)
      } else {
        if (waitingQueue.size < configuration.maxQueueSize) {
          waitingQueue.add(message.future)
          logger.trace { "no items available (${inUseItems.size} used), added to waiting queue (${waitingQueue.size} waiting)" }
        } else {
          logger.trace { "no items available (${inUseItems.size} used), and the waitQueue is full (${waitingQueue.size} waiting)" }
          message.future.completeExceptionally(PoolExhaustedException("There are no objects available and the waitQueue is full"))
        }
      }
    } catch (e: Exception) {
      message.future.completeExceptionally(e)
    }
  }

  private fun createObject(message: Take<T>) {
    val created = objectFactory.create()
    val itemId = itemIndex
    itemIndex++
    inCreateItems[itemId] = ObjectHolder(created)
    logger.trace { "createObject createRequest=$itemId" }
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

private open class PoolObjectHolder<T : PoolObject>(val item: T) {
  val time = System.currentTimeMillis()

  val timeElapsed: Long get() = System.currentTimeMillis() - time
}

private class ObjectHolder<T : Any>(val item: T) {
  val time = System.currentTimeMillis()

  val timeElapsed: Long get() = System.currentTimeMillis() - time
}

private data class ItemInUseHolder<T : PoolObject>(val itemInUse: T, val isInTest: Boolean, val testFuture: CompletableFuture<T>? = null) : PoolObjectHolder<T>(itemInUse)
