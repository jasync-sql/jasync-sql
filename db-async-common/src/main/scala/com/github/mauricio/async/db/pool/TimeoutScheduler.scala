package com.github.mauricio.async.db.pool

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{TimeUnit, TimeoutException, ScheduledFuture}
import com.github.mauricio.async.db.util.NettyUtils
import scala.concurrent.{ExecutionContext, Promise}
import scala.concurrent.duration.Duration

trait TimeoutScheduler {
  implicit val internalPool: ExecutionContext
  def onTimeout    // implementors should decide here what they want to do when a timeout occur
  private var isTimeoutedBool = new AtomicBoolean(false);
  def isTimeouted = isTimeoutedBool.get // We need this property as isClosed takes time to complete and
          // we don't want the connection to be used again.

  def addTimeout[A](promise: Promise[A], duration: Duration) : Option[ScheduledFuture[_]] = {
    if (duration != Duration.Inf) {
      val scheduledFuture = schedule(
        {
          if (promise.tryFailure(new TimeoutException(s"Operation is timeouted after it took too long to return (${duration})"))) {
            isTimeoutedBool.set(true)
            onTimeout
          }
        },
        duration)
      promise.future.onComplete(x => scheduledFuture.cancel(false))

      return Some(scheduledFuture)
    }
    return None
  }

  def schedule(block: => Unit, duration: Duration) : ScheduledFuture[_] =
    NettyUtils.DefaultEventLoopGroup.schedule(new Runnable {
      override def run(): Unit = block
    }, duration.toMillis, TimeUnit.MILLISECONDS)
}
