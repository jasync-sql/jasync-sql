package com.github.mauricio.postgresql.util

import java.util.concurrent.{ConcurrentLinkedQueue, ExecutionException, TimeoutException, TimeUnit}
import scala.collection.JavaConversions._

/**
 * User: Maurício Linhares
 * Date: 3/10/12
 * Time: 12:29 AM
 */

object SimpleFuture {
  val log = Log.get[SimpleFuture[Nothing]]
}

class SimpleFuture[T] extends Future[T] {

  import SimpleFuture._

  @volatile private var done = false
  @volatile private var result : T = _
  @volatile private var error : Throwable = null
  @volatile private var callbacks = new ConcurrentLinkedQueue[FutureResult[T] => Unit]()

  override def onComplete( fn : FutureResult[T] => Unit ) {
    this.callbacks.add( fn )
    if (this.isDone) {
      fn(this.getResult)
    }
  }

  override def isDone = this.done
  override def isError = this.error != null

  def set( value : T ) {
    this.result = value
    this.done = true
    this.fireCallbacks
  }

  def setError( error : Throwable ) {
    this.error = error
    this.done  = true
    this.fireCallbacks

    log.error("Received error", error)
  }

  override def get : T = {
    while ( !this.done ) {
      ThreadHelpers.safeSleep(500)
    }
    this.getValue
  }

  override def get( time : Long,  unit : TimeUnit ) : T = {
    val totalTime = unit.toMillis(time)
    val increment = 500
    var sum = 0

    while (!this.done && sum <= totalTime) {
      ThreadHelpers.safeSleep(increment)
      sum += increment
    }

    log.debug("Done is {}", this.done)

    if (this.done) {
      return this.getValue
    } else {
      throw new TimeoutException("Lock reached the timeout limit")
    }

  }

  private def fireCallbacks {

    val either = this.getResult

    for ( callback <- this.callbacks ) {
      callback(either)
    }
  }

  private def getValue : T = {
    if ( this.error != null ) {
      throw new ExecutionException(this.error)
    } else {
      this.result
    }
  }

  private def getResult : FutureResult[T] = {
    if ( this.error != null ) {
      FutureFailure(this.error)
    } else {
      FutureSuccess(this.result)
    }
  }

}
