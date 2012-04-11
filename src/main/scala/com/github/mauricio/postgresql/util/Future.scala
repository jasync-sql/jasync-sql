package com.github.mauricio.postgresql.util

import java.util.concurrent.TimeUnit

/**
 * User: Maurício Linhares
 * Date: 3/10/12
 * Time: 6:44 PM
 */

trait Future[L,R] {

  def get : Either[L,R]
  def get( time : Long,  unit : TimeUnit ) : Either[L,R]
  def isDone : Boolean
  def isError : Boolean
  def onComplete( fn : Either[L,R] => Unit )

}
