package com.github.jasync.sql.db.pool

import com.github.jasync.sql.db.util.nullableMap
import com.github.jasync.sql.db.util.onCompleteAsync
import com.github.jasync.sql.db.util.tryFailure
import io.netty.channel.EventLoopGroup
import mu.KotlinLogging
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

private val logger = KotlinLogging.logger {}

interface TimeoutScheduler {

    /**
     *
     * We need this method because isClosed takes time to complete and
     * we don't want the connection to be used again.
     *
     * @return true if connection has a query that time-out
     */

    fun isTimeout(): Boolean

}

class TimeoutSchedulerImpl(
    private val executor: Executor,
    private val eventLoopGroup: EventLoopGroup,
    private val timeoutFun: () -> Unit

) : TimeoutScheduler {

    /**
     * Implementors should decide here what they want to do when a timeout occur
     */
    private fun onTimeout() {
        timeoutFun()
    }

    private var isTimeoutBool = AtomicBoolean(false)

    override fun isTimeout(): Boolean = isTimeoutBool.get()

    private fun schedule(block: () -> Unit, duration: Duration): ScheduledFuture<*> {
        return eventLoopGroup.schedule({
            block()
        }, duration.toMillis(), TimeUnit.MILLISECONDS)
    }

    fun <A> addTimeout(promise: CompletableFuture<A>, durationOption: Duration?, connectionId: String): ScheduledFuture<*>? {
        return durationOption.nullableMap { duration ->
            logger.trace { "adding timeout $duration for connectionId $connectionId" }
            val scheduledFuture = schedule(
                {
                    if (promise.tryFailure(TimeoutException("Operation is timeouted after it took too long to return ($duration)"))) {
                        logger.trace { "operation timeouted for connectionId $connectionId" }
                        isTimeoutBool.set(true)
                        onTimeout()
                    }
                },
                duration
            )
            promise.onCompleteAsync(executor) { scheduledFuture.cancel(false) }
            scheduledFuture
        }
    }

}
