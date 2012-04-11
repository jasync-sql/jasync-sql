package com.github.mauricio.postgresql.util

import java.util.concurrent.{ConcurrentLinkedQueue, TimeoutException, TimeUnit}
import scala.collection.JavaConversions._

/**
 * User: Maurício Linhares
 * Date: 3/10/12
 * Time: 12:29 AM
 */

object SimpleFuture {
  val log = Log.get[SimpleFuture[Throwable,Nothing]]
}

class SimpleFuture[L >: Throwable,R] extends Future[L,R] {

  import SimpleFuture._

  @volatile private var done = false
  @volatile private var result : R = _
  @volatile private var error : L = _
  @volatile private var callbacks = new ConcurrentLinkedQueue[Either[L,R] => Unit]()

  override def onComplete( fn : Either[L,R] => Unit ) {
    this.callbacks.add( fn )
    if (this.isDone) {
      fn(this.getResult)
    }
  }

  override def isDone = this.done
  override def isError = this.error != null

  def set( value : R ) {
    this.result = value
    this.done = true
    this.fireCallbacks
  }

  def setError( error : L ) {
    this.error = error
    this.done  = true
    this.fireCallbacks

    log.error("Received error", error)
  }

  override def get : Either[L,R] = {
    while ( !this.done ) {
      ThreadHelpers.safeSleep(500)
    }
    this.getResult
  }

  override def get( time : Long,  unit : TimeUnit ) : Either[L,R] = {
    val totalTime = unit.toMillis(time)
    val increment = 500
    var sum = 0

    while (!this.done && sum <= totalTime) {
      ThreadHelpers.safeSleep(increment)
      sum += increment
    }

    log.debug("Done is {}", this.done)

    if (this.done) {
      return this.getResult
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

  private def getResult : Either[L,R] = {
    if ( this.error != null ) {
      Left(this.error)
    } else {
      Right(this.result)
    }
  }

}
