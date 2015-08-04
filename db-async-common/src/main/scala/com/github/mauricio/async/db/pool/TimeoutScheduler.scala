package com.github.mauricio.async.db.pool

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{TimeUnit, TimeoutException, ScheduledFuture}
import io.netty.channel.EventLoopGroup
import scala.concurrent.{ExecutionContext, Promise}
import scala.concurrent.duration.Duration

trait TimeoutScheduler {

  private var isTimeoutedBool = new AtomicBoolean(false)

  /**
   *
   * The event loop group to be used for scheduling.
   *
   * @return
   */

  def eventLoopGroup : EventLoopGroup

  /**
   * Implementors should decide here what they want to do when a timeout occur
   */

  def onTimeout    // implementors should decide here what they want to do when a timeout occur

  /**
   *
   * We need this property as isClosed takes time to complete and
   * we don't want the connection to be used again.
   *
   * @return
   */

  def isTimeouted : Boolean =
    isTimeoutedBool.get

  def addTimeout[A](
                     promise: Promise[A],
                     durationOption: Option[Duration])
                   (implicit executionContext : ExecutionContext) : Option[ScheduledFuture[_]] = {
    durationOption.map {
      duration =>
        val scheduledFuture = schedule(
        {
          if (promise.tryFailure(new TimeoutException(s"Operation is timeouted after it took too long to return (${duration})"))) {
            isTimeoutedBool.set(true)
            onTimeout
          }
        },
        duration)
        promise.future.onComplete(x => scheduledFuture.cancel(false))

        scheduledFuture
    }
  }

  def schedule(block: => Unit, duration: Duration) : ScheduledFuture[_] =
    eventLoopGroup.schedule(new Runnable {
      override def run(): Unit = block
    }, duration.toMillis, TimeUnit.MILLISECONDS)
}
