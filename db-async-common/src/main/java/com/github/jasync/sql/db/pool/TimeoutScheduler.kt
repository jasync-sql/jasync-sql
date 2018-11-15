package com.github.jasync.sql.db.pool

import com.github.jasync.sql.db.util.nullableMap
import com.github.jasync.sql.db.util.onCompleteAsync
import com.github.jasync.sql.db.util.tryFailure
import io.netty.channel.EventLoopGroup
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledFuture
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

  fun onTimeout(): Unit

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

class TimeoutSchedulerImpl(private val executor: Executor,
                           private val group: EventLoopGroup,
                           private val timeoutFun: () -> Unit

) : TimeoutScheduler {
  override fun eventLoopGroup(): EventLoopGroup {
    return group
  }

  override fun onTimeout() {
    timeoutFun()
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
      promise.onCompleteAsync(executor) { scheduledFuture.cancel(false) }
      scheduledFuture
    }
  }

}
