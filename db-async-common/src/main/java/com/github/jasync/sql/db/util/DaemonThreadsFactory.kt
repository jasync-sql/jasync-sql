
package com.github.jasync.sql.db.util

import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

data class DaemonThreadsFactory(val name: String) : ThreadFactory {

  private val threadNumber = AtomicInteger(1)

  //TODO - where is it used
  override fun newThread(r: Runnable): Thread {
    val thread = Executors.defaultThreadFactory().newThread(r)
    thread.setDaemon(true)
    val threadName = name + "-thread-" + threadNumber.getAndIncrement()
    thread.setName(threadName)
    return thread
  }
}
