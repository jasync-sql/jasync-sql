package com.github.mauricio.async.db.pool

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.ScheduledFuture
import io.netty.channel.EventLoopGroup
//import scala.concurrent.ExecutionContext
//import scala.concurrent.Promise
//import scala.concurrent.duration.Duration

interface TimeoutScheduler {

//  private var isTimeoutedBool = AtomicBoolean(false)

  /**
   *
   * The event loop group to be used for scheduling.
   *
   * @return
   */

  fun eventLoopGroup (): EventLoopGroup

  /**
   * Implementors should decide here what they want to do when a timeout occur
   */

  fun onTimeout()    // implementors should decide here what they want to do when a timeout occur

  /**
   *
   * We need this property as isClosed takes time to complete and
   * we don't want the connection to be used again.
   *
   * @return
   */

//  fun isTimeouted (): Boolean =
//    isTimeoutedBool.get
//
//  fun addTimeout<A>(
//                     promise: Promise<A>,
//                     durationOption: Option<Duration>)
//                   (implicit executionContext : ExecutionContext) : Option<ScheduledFuture<_>> {
//    durationOption.map {
//      duration ->
//        val scheduledFuture = schedule(
//        {
//          if (promise.tryFailure(TimeoutException(s"Operation is timeouted after it took too long to return (${duration})"))) {
//            isTimeoutedBool.set(true)
//            onTimeout
//          }
//        },
//        duration)
//        promise.future.onComplete(x -> scheduledFuture.cancel(false))
//
//        scheduledFuture
//    }
//  }
//
//  fun schedule(block: -> Unit, duration: Duration) : ScheduledFuture<_> =
//    eventLoopGroup.schedule(Runnable {
//      override fun run(): Unit = block
//    }, duration.toMillis, TimeUnit.MILLISECONDS)
}
