package com.github.jasync.sql.db.pool

import com.github.jasync.sql.db.util.Failure
import com.github.jasync.sql.db.util.Success
import com.github.jasync.sql.db.util.Try
import com.github.jasync.sql.db.util.XXX
import com.github.jasync.sql.db.util.failed
import com.github.jasync.sql.db.util.map
import com.github.jasync.sql.db.util.mapTry
import com.github.jasync.sql.db.util.onComplete
import java.util.LinkedList
import java.util.Queue
import java.util.WeakHashMap
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

// consider ticker channel when its stable
// https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/ticker.html
// https://github.com/Kotlin/kotlinx.coroutines/blob/master/docs/channels.md#ticker-channels
object TestConnectionScheduler {
    private val executor: ScheduledExecutorService by lazy {
        Executors.newSingleThreadScheduledExecutor { r ->
            val t = Executors.defaultThreadFactory().newThread(r)
            t.isDaemon = true
            t
        }
    }

    fun scheduleAtFixedRate(periodMillis: Long, task: () -> Unit): ScheduledFuture<*> {
        return executor.scheduleAtFixedRate(task, periodMillis, periodMillis, TimeUnit.MILLISECONDS)
    }
}

@Suppress("EXPERIMENTAL_API_USAGE")
class ActorBasedObjectPool<T : PooledObject>
internal constructor(
    objectFactory: ObjectFactory<T>,
    configuration: PoolConfiguration,
    testItemsPeriodically: Boolean,
    extraTimeForTimeoutCompletion: Long = TimeUnit.SECONDS.toMillis(30)
) : AsyncObjectPool<T>, CoroutineScope {

    @Suppress("unused", "RedundantVisibilityModifier")
    public constructor(
        objectFactory: ObjectFactory<T>,
        configuration: PoolConfiguration
    ) : this(objectFactory, configuration, true)

    private val job = SupervisorJob() + configuration.coroutineDispatcher
    override val coroutineContext: CoroutineContext get() = job

    var closed = false
    private var testItemsFuture: ScheduledFuture<*>? = null

    init {
        if (testItemsPeriodically) {
            logger.info { "registering pool for periodic connection tests $this - $configuration" }
            testItemsFuture =
                TestConnectionScheduler.scheduleAtFixedRate(configuration.validationInterval) {
                    try {
                        testAvailableItems()
                    } catch (t: Throwable) {
                        logger.debug(t) { "got exception when testing items" }
                    }
                }
        }
    }

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

    override fun giveBack(item: T): CompletableFuture<AsyncObjectPool<T>> {
        val future = CompletableFuture<Unit>()
        val offered = actor.offer(GiveBack(item, future))
        if (!offered) {
            future.completeExceptionally(Exception("could not offer to actor"))
        }
        return future.map { this }
    }

    override fun close(): CompletableFuture<AsyncObjectPool<T>> {
        if (closed) {
            return CompletableFuture.completedFuture(this)
        }
        logger.info { "closing the pool" }
        closed = true
        val future = CompletableFuture<Unit>()
        val offered = actor.offer(Close(future))
        if (!offered) {
            future.completeExceptionally(Exception("could not offer to actor"))
        }
        testItemsFuture?.cancel(true)
        return future.map {
            job.cancel()
            this
        }
    }

    fun testAvailableItems() {
        if (closed) {
            logger.trace { "testAvailableItems - not working, pool is closed" }
            return
        }
        logger.trace { "testAvailableItems - starting" }
        val offered = actor.offer(TestPoolItems())
        if (!offered) {
            logger.warn { "failed to offer to actor - testAvailableItems()" }
        }
    }

    private val actorInstance =
        ObjectPoolActor(objectFactory, configuration, extraTimeForTimeoutCompletion) {
            actor
        }

    private val actor: SendChannel<ActorObjectPoolMessage<T>> = actor(
        context = configuration.coroutineDispatcher,
        capacity = Int.MAX_VALUE,
        start = CoroutineStart.DEFAULT,
        onCompletion = null
    ) {
        for (message in channel) {
            try {
                actorInstance.onReceive(message)
            } catch (t: Throwable) {
                logger.warn(t) { "uncaught Throwable" }
            }
        }
    }

    val availableItems: List<T> get() = actorInstance.availableItemsList
    val usedItems: List<T> get() = actorInstance.usedItemsList
    val waitingForItem: List<CompletableFuture<T>> get() = actorInstance.waitingForItemList
    val usedItemsSize: Int get() = actorInstance.usedItemsSize
    val waitingForItemSize: Int get() = actorInstance.waitingForItemSize
    val availableItemsSize: Int get() = actorInstance.availableItemsSize
}

@Suppress("unused")
private sealed class ActorObjectPoolMessage<T : PooledObject> {
    override fun toString(): String {
        return "${javaClass.simpleName} @${hashCode()}"
    }
}

private class Take<T : PooledObject>(val future: CompletableFuture<T>) : ActorObjectPoolMessage<T>()
private class GiveBack<T : PooledObject>(
    val returnedItem: T,
    val future: CompletableFuture<Unit>,
    val exception: Throwable? = null,
    val originalTime: Long? = null
) : ActorObjectPoolMessage<T>() {
    override fun toString(): String {
        return "GiveBack: ${returnedItem.id} hasError=" +
            if (exception != null)
                "${exception.javaClass.simpleName} - ${exception.message}"
            else "false"
    }
}

private class Created<T : PooledObject>(
    val itemCreateId: Int,
    val item: Try<T>,
    val takeAskFuture: CompletableFuture<T>?
) : ActorObjectPoolMessage<T>() {
    override fun toString(): String {
        val id = when (item) {
            is Success<T> -> item.value.id
            else -> "failed"
        }
        return "Created: createRequest=$itemCreateId -> object=$id"
    }
}

private class TestPoolItems<T : PooledObject> : ActorObjectPoolMessage<T>()
private class Close<T : PooledObject>(val future: CompletableFuture<Unit>) :
    ActorObjectPoolMessage<T>()

@Suppress("REDUNDANT_ELSE_IN_WHEN")
private class ObjectPoolActor<T : PooledObject>(
    private val objectFactory: ObjectFactory<T>,
    private val configuration: PoolConfiguration,
    private val extraTimeForTimeoutCompletion: Long,
    private val channelProvider: () -> SendChannel<ActorObjectPoolMessage<T>>
) {

    private val availableItems: Queue<PoolObjectHolder<T>> = LinkedList()
    private val waitingQueue: Queue<ObjectHolder<CompletableFuture<T>>> = LinkedList()
    private val inUseItems = WeakHashMap<T, ItemInUseHolder<T>>()
    private val inCreateItems = mutableMapOf<Int, ObjectHolder<CompletableFuture<out T>>>()
    private var createIndex = 0
    private val channel: SendChannel<ActorObjectPoolMessage<T>> by lazy { channelProvider() }

    val availableItemsList: List<T> get() = availableItems.map { it.item }
    val usedItemsList: List<T> get() = inUseItems.keys.toList()
    val waitingForItemList: List<CompletableFuture<T>> get() = waitingQueue.toList().map { it.item }
    val usedItemsSize: Int get() = inUseItems.size
    val waitingForItemSize: Int get() = waitingQueue.size
    val availableItemsSize: Int get() = availableItems.size

    var closed = false

    fun onReceive(message: ActorObjectPoolMessage<T>) {
        logger.trace { "received message: $message ; $poolStatusString" }
        when (message) {
            is Take<T> -> handleTake(message)
            is GiveBack<T> -> handleGiveBack(message)
            is Created<T> -> handleCreated(message)
            is TestPoolItems<T> -> handleTestPoolItems()
            is Close<T> -> handleClose(message)
            else -> XXX("no handle for message $message")
        }
        scheduleNewItemsIfNeeded()
    }

    private fun scheduleNewItemsIfNeeded() {
        logger.trace { "scheduleNewItemsIfNeeded - $poolStatusString" }
        // deal with inconsistency in case we have items but also waiting futures
        while (availableItems.size > 0 && waitingQueue.isNotEmpty()) {
            val futureHolder = waitingQueue.peek()
            val wasBorrowed = borrowFirstAvailableItem(futureHolder.item)
            if (wasBorrowed) {
                waitingQueue.remove()
                logger.trace { "scheduleNewItemsIfNeeded - borrowed object ; $poolStatusString" }
                return
            }
        }
        // deal with inconsistency in case we have waiting futures, and but we can create new items for them
        while (availableItems.isEmpty() &&
            waitingQueue.isNotEmpty() &&
            totalItems < configuration.maxObjects &&
            waitingQueue.size > inCreateItems.size
        ) {
            createObject(null)
            logger.trace { "scheduleNewItemsIfNeeded - creating new object ; $poolStatusString" }
        }
    }

    private val poolStatusString: String
        get() =
            "availableItems=${availableItems.size} waitingQueue=${waitingQueue.size} inUseItems=${inUseItems.size} inCreateItems=${inCreateItems.size} ${this.channel}"

    private fun handleClose(message: Close<T>) {
        try {
            closed = true
            channel.close()
            availableItems.forEach { it.item.destroy() }
            availableItems.clear()
            inUseItems.forEach {
                it.value.cleanedByPool = true
                it.key.destroy()
            }
            inUseItems.clear()
            waitingQueue.forEach { it.item.completeExceptionally(PoolAlreadyTerminatedException()) }
            waitingQueue.clear()
            inCreateItems.values.forEach {
                it.item.completeExceptionally(PoolAlreadyTerminatedException())
            }
            inCreateItems.clear()
            message.future.complete(Unit)
        } catch (e: Exception) {
            message.future.completeExceptionally(e)
        }
    }

    private fun handleTestPoolItems() {
        sendAvailableItemsToTest()
        checkItemsInCreationForTimeout()
        checkItemsInTestOrQueryForTimeout()
        checkWaitingFuturesForTimeout()
        logger.trace { "testAvailableItems - done testing" }
    }

    private fun checkWaitingFuturesForTimeout() {
        val queryTimeout = configuration.queryTimeout
        // no timeout
            ?: return
        while (!waitingQueue.isEmpty()) {
            val futureHolder = waitingQueue.peek()
            if (futureHolder.timeElapsed > queryTimeout) {
                logger.trace { "checkWaitingFuturesForTimeout - timeout waiting future after ${futureHolder.timeElapsed} ms" }
                // should timeout future
                waitingQueue.remove()
                futureHolder.item.completeExceptionally(TimeoutException("timeout while waiting in queue after ${futureHolder.timeElapsed} ms"))
            } else {
                // we assume that once we found a future that should still wait
                // all futures after him should also wait because they arrived later
                return
            }
        }
    }

    private fun checkItemsInTestOrQueryForTimeout() {
        inUseItems.entries.removeAll { entry ->
            val holder = entry.value
            val item = entry.key
            var itemWasTimeout = false
            if (holder.isInTest && holder.timeElapsed > configuration.testTimeout) {
                logger.trace { "failed to test item ${item.id} after ${holder.timeElapsed} ms, will destroy it" }
                holder.cleanedByPool = true
                item.destroy()
                holder.testFuture!!.completeExceptionally(TimeoutException("failed to test item ${item.id} after ${holder.timeElapsed} ms"))
                itemWasTimeout = true
            }
            if (!holder.isInTest && configuration.queryTimeout != null &&
                holder.timeElapsed > configuration.queryTimeout + extraTimeForTimeoutCompletion
            ) {
                logger.error { "timeout query item ${item.id} after ${holder.timeElapsed} ms and was not cleaned by connection as it should, will destroy it - timeout is ${configuration.queryTimeout}" }
                holder.cleanedByPool = true
                item.destroy()
                itemWasTimeout = true
            }
            itemWasTimeout
        }
    }

    private fun checkItemsInCreationForTimeout() {
        inCreateItems.entries.removeAll {
            val timeout = it.value.timeElapsed > configuration.createTimeout
            if (timeout) {
                logger.trace { "failed to create item ${it.key} after ${it.value.timeElapsed} ms" }
                it.value.item.completeExceptionally(TimeoutException("failed to create item ${it.key} after ${it.value.timeElapsed} ms"))
            }
            timeout
        }
    }

    private fun T.destroy() {
        logger.trace { "destroy item ${this.id}" }
        objectFactory.destroy(this)
    }

    private fun sendAvailableItemsToTest() {
        availableItems.forEach {
            val item = it.item
            logger.trace { "test: ${item.id} available ${it.timeElapsed} ms" }
            when {
                it.timeElapsed > configuration.maxIdle -> {
                    logger.trace { "releasing idle item ${item.id}" }
                    item.destroy()
                }
                configuration.maxObjectTtl != null && System.currentTimeMillis() - item.creationTime > configuration.maxObjectTtl -> {
                    logger.trace { "releasing item past ttl ${item.id}" }
                    item.destroy()
                }
                else -> {
                    val test = objectFactory.test(item)
                    inUseItems[item] = ItemInUseHolder(item.id, isInTest = true, testFuture = test)
                    test.mapTry { _, t ->
                        offerOrLog(GiveBack(item, CompletableFuture(), t, originalTime = it.time)) {
                            "test item"
                        }
                    }
                }
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
        val removed = inCreateItems.remove(message.itemCreateId)
        if (removed == null) {
            logger.warn { "could not find connection ${message.itemCreateId}" }
        }
        val future = message.takeAskFuture
        if (future == null) {
            when (message.item) {
                is Failure -> logger.debug { "failed to create connection, with no callback attached " }
                is Success -> {
                    availableItems.add(PoolObjectHolder(message.item.value))
                }
            }
        } else {
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
    }

    private fun T.borrowTo(future: CompletableFuture<T>, validate: Boolean = true) {
        if (validate) {
            validate(this)
        }
        inUseItems[this] = ItemInUseHolder(this.id, isInTest = false)
        logger.trace { "borrowed: ${this.id} ; $poolStatusString" }
        future.complete(this)
    }

    private fun handleGiveBack(message: GiveBack<T>) {
        try {
            val removed = inUseItems.remove(message.returnedItem)
            removed?.apply { cleanedByPool = true }
            if (removed == null) {
                val isFromOurPool: Boolean =
                    this.availableItems.any { holder -> message.returnedItem === holder.item }
                logger.trace { "give back got item not in use: ${message.returnedItem.id} isFromOurPool=$isFromOurPool ; $poolStatusString" }
                if (isFromOurPool) {
                    message.future.failed(IllegalStateException("This item has already been returned"))
                } else {
                    message.future.failed(IllegalArgumentException("The returned item did not come from this pool."))
                }
                return
            }
            if (message.exception != null) {
                logger.trace { "GiveBack got exception, so destroying item ${message.returnedItem.id}, exception is ${message.exception.javaClass.simpleName} - ${message.exception.message}" }
                throw message.exception
            }
            validate(message.returnedItem)
            message.future.complete(Unit)
            if (waitingQueue.isEmpty()) {
                if (availableItems.any { holder -> message.returnedItem === holder.item }) {
                    logger.warn { "trying to give back an item to the pool twice ${message.returnedItem.id}, will ignore that" }
                    return
                }
                availableItems.add(
                    when (message.originalTime) {
                        null -> PoolObjectHolder(message.returnedItem)
                        else -> PoolObjectHolder(message.returnedItem, message.originalTime)
                    }
                )
                logger.trace { "add ${message.returnedItem.id} to available items, size is ${availableItems.size}" }
            } else {
                val waitingFuture = waitingQueue.remove()
                message.returnedItem.borrowTo(waitingFuture.item, validate = false)
            }
        } catch (e: Throwable) {
            logger.trace(e) { "GiveBack caught exception, so destroying item ${message.returnedItem.id} " }
            try {
                message.returnedItem.destroy()
            } catch (e1: Throwable) {
                logger.trace(e1) { "GiveBack caught exception, destroy also caught exception ${message.returnedItem.id} " }
            }
            message.future.completeExceptionally(e)
        }
    }

    private fun handleTake(message: Take<T>) {
        // take from available
        while (availableItems.isNotEmpty()) {
            val future = message.future
            val wasBorrowed = borrowFirstAvailableItem(future)
            if (wasBorrowed)
                return
        }
        // available is empty
        createNewItemPutInWaitQueue(message)
    }

    private fun borrowFirstAvailableItem(future: CompletableFuture<T>): Boolean {
        val itemHolder = availableItems.remove()
        try {
            validateTtl(itemHolder.item)
            itemHolder.item.borrowTo(future)
            return true
        } catch (e: Exception) {
            logger.debug { "validation of object '${itemHolder.item.id}' failed, removing it from pool: ${e.message}" }
            itemHolder.item.destroy()
        }
        return false
    }

    private fun validateTtl(item: T) {
        val age = System.currentTimeMillis() - item.creationTime
        if (configuration.maxObjectTtl != null && age > configuration.maxObjectTtl) {
            throw MaxTtlPassedException(item.id, age, configuration.maxObjectTtl)
        }
    }

    private val totalItems: Int get() = inUseItems.size + inCreateItems.size + availableItems.size

    private fun createNewItemPutInWaitQueue(message: Take<T>) {
        try {
            if (totalItems < configuration.maxObjects) {
                createObject(message.future)
            } else {
                if (waitingQueue.size < configuration.maxQueueSize) {
                    waitingQueue.add(ObjectHolder(message.future))
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

    private fun createObject(future: CompletableFuture<T>?) {
        val created = objectFactory.create()
        val itemCreateId = createIndex
        createIndex++
        inCreateItems[itemCreateId] = ObjectHolder(created)
        logger.trace { "createObject createRequest=$itemCreateId" }
        created.onComplete { tried ->
            offerOrLog(Created(itemCreateId, tried, future)) {
                "failed to offer on created item $itemCreateId"
            }
        }
    }

    private fun validate(item: T) {
        when (val tried = objectFactory.validate(item)) {
            is Failure -> throw tried.exception
        }
    }
}

private open class PoolObjectHolder<T : PooledObject>(
    val item: T,
    val time: Long = System.currentTimeMillis()
) {

    val timeElapsed: Long get() = System.currentTimeMillis() - time
}

private class ObjectHolder<T : Any>(val item: T) {
    val time = System.currentTimeMillis()

    val timeElapsed: Long get() = System.currentTimeMillis() - time
}

private data class ItemInUseHolder<T : PooledObject>(
    val itemId: String,
    val isInTest: Boolean,
    val testFuture: CompletableFuture<T>? = null,
    val time: Long = System.currentTimeMillis(),
    var cleanedByPool: Boolean = false

) {
    val timeElapsed: Long get() = System.currentTimeMillis() - time

    @Suppress("unused", "ProtectedInFinal")
    protected fun finalize() {
        if (!cleanedByPool) {
            logger.warn { "LEAK DETECTED for item $this - $timeElapsed ms since in use" }
        }
    }
}
