package com.github.jasync.sql.db.util

import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

data class DaemonThreadsFactory(val name: String) : ThreadFactory {

    private val threadNumber = AtomicInteger(1)

    override fun newThread(r: Runnable): Thread {
        val thread = Executors.defaultThreadFactory().newThread(r)
        thread.isDaemon = true
        val threadName = name + "-thread-" + threadNumber.getAndIncrement()
        thread.name = threadName
        return thread
    }
}
