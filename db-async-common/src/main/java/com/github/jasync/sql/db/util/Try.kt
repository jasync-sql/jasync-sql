package com.github.jasync.sql.db.util

/**
 * The `Try` type represents a computation that may either result in an exception, or return a
 * successfully computed value.
 *
 * Port of https://github.com/scala/scala/blob/v2.12.1/src/library/scala/util/Try.scala
 */
sealed class Try<out A> {

  companion object {

    fun <A> just(a: A): Try<A> = Success(a)
    fun <A> raise(e: Throwable): Try<A> = Failure(e)

    inline operator fun <A> invoke(f: () -> A): Try<A> =
        try {
          Success(f())
        } catch (e: Throwable) {
          Failure(e)
        }
  }

  abstract val isFailure: Boolean
  abstract val isSuccess: Boolean
  abstract fun get(): A

}

/**
 * The `Failure` type represents a computation that result in an exception.
 */
data class Failure(val exception: Throwable) : Try<Nothing>() {
  override val isFailure: Boolean = true
  override val isSuccess: Boolean = false
  override fun get(): Nothing {
    throw exception
  }
}

/**
 * The `Success` type represents a computation that return a successfully computed value.
 */
data class Success<out A>(val value: A) : Try<A>() {
  override val isFailure: Boolean = false
  override val isSuccess: Boolean = true
  override fun get(): A = value
}

