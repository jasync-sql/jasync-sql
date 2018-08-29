package com.github.jasync.sql.db.pool

import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.util.XXX
import com.github.jasync.sql.db.util.nullableMap
import com.github.jasync.sql.db.util.onComplete
import com.github.jasync.sql.db.util.tryFailure
import io.netty.channel.EventLoopGroup
import io.netty.util.concurrent.ScheduledFuture
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

interface TimeoutScheduler {


  /**
   *
   * The event loop group to be used for scheduling.
   *
   * @return
   */

  fun eventLoopGroup(): EventLoopGroup

  /**
   * Implementors should decide here what they want to do when a timeout occur
   */

  fun onTimeout(): CompletableFuture<Connection>    // implementors should decide here what they want to do when a timeout occur

  /**
   *
   * We need this property as isClosed takes time to complete and
   * we don't want the connection to be used again.
   *
   * @return
   */

  fun isTimeout(): Boolean

  fun <A> addTimeout(promise: CompletableFuture<A>, durationOption: Duration?): ScheduledFuture<*>?

  fun schedule(block: () -> Unit, duration: Duration): ScheduledFuture<*>
}

class TimeoutSchedulerPartialImpl(private val executor: Executor) : TimeoutScheduler {
  override fun eventLoopGroup(): EventLoopGroup {
    XXX("should be implemented in subclass")
  }

  override fun onTimeout(): CompletableFuture<Connection> {
    XXX("should be implemented in subclass")
  }

  private var isTimeoutBool = AtomicBoolean(false)

  override fun isTimeout(): Boolean = isTimeoutBool.get()

  override fun schedule(block: () -> Unit, duration: Duration): ScheduledFuture<*> {
    return eventLoopGroup().schedule({
      block()
    }, duration.toMillis(), TimeUnit.MILLISECONDS)
  }

  override fun <A> addTimeout(promise: CompletableFuture<A>, durationOption: Duration?): ScheduledFuture<*>? {
    return durationOption.nullableMap { duration ->
      val scheduledFuture = schedule(
          {
            if (promise.tryFailure(TimeoutException("Operation is timeouted after it took too long to return ($duration)"))) {
              isTimeoutBool.set(true)
              onTimeout()
            }
          },
          duration)
      promise.onComplete(executor) { _ -> scheduledFuture.cancel(false) }
      scheduledFuture
    }
  }

}
