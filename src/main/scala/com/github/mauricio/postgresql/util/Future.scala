package com.github.mauricio.postgresql.util

import java.util.concurrent.TimeUnit

/**
 * User: Maurício Linhares
 * Date: 3/10/12
 * Time: 6:44 PM
 */

trait Future[T] {

  def get : T
  def get( time : Long,  unit : TimeUnit ) : T
  def isDone : Boolean
  def isError : Boolean
  def onComplete( fn : FutureResult[T] => Unit )

}
