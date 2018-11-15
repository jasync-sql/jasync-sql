package com.github.jasync.sql.db.pool

import com.github.jasync.sql.db.util.ExecutorServiceUtils
import com.github.jasync.sql.db.util.NettyUtils
import io.netty.channel.EventLoopGroup
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.atomic.AtomicInteger

/**
 * Implementation of TimeoutScheduler used for testing
 */
class DummyTimeoutScheduler : TimeoutScheduler {

    private val timeoutSchedulerImpl = TimeoutSchedulerImpl(ExecutorServiceUtils.CommonPool, NettyUtils.DefaultEventLoopGroup, this::onTimeout)

    override fun <A> addTimeout(promise: CompletableFuture<A>, durationOption: Duration?): ScheduledFuture<*>? {
        return timeoutSchedulerImpl.addTimeout(promise, durationOption)
    }

    override fun schedule(block: () -> Unit, duration: Duration): ScheduledFuture<*> {
        return timeoutSchedulerImpl.schedule(block, duration)
    }

    private val timeOuts = AtomicInteger()

    override fun onTimeout() {
        timeOuts.incrementAndGet()
    }

    override fun eventLoopGroup(): EventLoopGroup {
        return NettyUtils.DefaultEventLoopGroup
    }

    fun timeOutCount(): Int {
        return timeOuts.get()
    }

    override fun isTimeout(): Boolean {
        return timeOuts.get() > 0
    }
}
