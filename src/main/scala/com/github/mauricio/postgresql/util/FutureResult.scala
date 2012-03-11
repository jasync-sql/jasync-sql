package com.github.mauricio.postgresql.util

/**
 * User: Maurício Linhares
 * Date: 3/10/12
 * Time: 10:41 AM
 */

sealed abstract class FutureResult[+S] {

  def apply() : S = this.get
  def isSuccess : Boolean
  def isFailure : Boolean
  def get : S
  def getFailure : Throwable

  def fold[R]( failure : Throwable => R, success : S => R ) = this match {
    case FutureSuccess( a ) => success(a)
    case FutureFailure( e ) => failure(e)
  }

}

final case class FutureSuccess[+S]( value : S ) extends FutureResult[S] {

  override def isSuccess : Boolean = true
  override def isFailure : Boolean = false
  override def get = value
  override def getFailure = throw new IllegalStateException("This option is a success, there is no failure here")

}

final case class FutureFailure[+S]( value : Throwable ) extends FutureResult[S] {

  override def isSuccess : Boolean = true
  override def isFailure : Boolean = false
  override def get = throw new IllegalStateException("This option is an error, there is no success here", value)
  override def getFailure = value

}