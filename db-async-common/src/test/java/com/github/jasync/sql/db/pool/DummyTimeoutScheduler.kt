package com.github.jasync.sql.db.pool

import com.github.jasync.sql.db.util.ExecutorServiceUtils
import com.github.jasync.sql.db.util.NettyUtils
import io.netty.channel.EventLoopGroup
import java.util.concurrent.atomic.AtomicInteger

/**
 * Implementation of TimeoutScheduler used for testing
 */
class DummyTimeoutScheduler : TimeoutSchedulerPartialImpl(ExecutorServiceUtils.CachedThreadPool) {

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
