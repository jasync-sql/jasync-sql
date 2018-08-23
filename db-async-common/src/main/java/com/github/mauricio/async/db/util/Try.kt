package com.github.mauricio.async.db.util

typealias Failure = Try.Failure
typealias Success<A> = Try.Success<A>

/**
 * The `Try` type represents a computation that may either result in an exception, or return a
 * successfully computed value.
 *
 * Port of https://github.com/scala/scala/blob/v2.12.1/src/library/scala/util/Try.scala
 */
sealed class Try<out A> {

  companion object {

    fun <A> just(a: A): Try<A> = Success(a)



    inline operator fun <A> invoke(f: () -> A): Try<A> =
        try {
          Success(f())
        } catch (e: Throwable) {
          Failure(e)
        }

    fun <A> raise(e: Throwable): Try<A> = Failure(e)

  }

  //@Deprecated(DeprecatedUnsafeAccess, ReplaceWith("getOrElse { ifEmpty }"))
  operator fun invoke() = get()


  /**
   * Returns the given function applied to the value from this `Success` or returns this if this is a `Failure`.
   */
  inline fun <B> flatMap(f: (A) -> Try<B>): Try<B> =
      when (this) {
        is Failure -> this
        is Success -> f(value)
      }

  /**
   * Maps the given function to the value from this `Success` or returns this if this is a `Failure`.
   */
  inline fun <B> map(f: (A) -> B): Try<B> =
      flatMap { Success(f(it)) }

  /**
   * Converts this to a `Failure` if the predicate is not satisfied.
   */
  fun filter(p: (A) -> Boolean): Try<A> =
      flatMap { if (p(it)) Success(it) else Failure(TryException.PredicateException("Predicate does not hold for $it")) }

  /**
   * Inverts this `Try`. If this is a `Failure`, returns its exception wrapped in a `Success`.
   * If this is a `Success`, returns a `Failure` containing an `UnsupportedOperationException`.
   */
  fun failed(): Try<Throwable> =
      fold(
          { Success(it) },
          { Failure(TryException.UnsupportedOperationException("Success")) }
      )

  /**
   * Applies `ifFailure` if this is a `Failure` or `ifSuccess` if this is a `Success`.
   */
  inline fun <B> fold(ifFailure: (Throwable) -> B, ifSuccess: (A) -> B): B =
      when (this) {
        is Failure -> ifFailure(exception)
        is Success -> ifSuccess(value)
      }

  abstract fun isFailure(): Boolean

  abstract fun isSuccess(): Boolean

  //@Deprecated(DeprecatedUnsafeAccess, ReplaceWith("fold({ Unit }, f)"))
  fun foreach(f: (A) -> Unit) {
    if (isSuccess()) f(get())
  }

  //@Deprecated(DeprecatedUnsafeAccess, ReplaceWith("map { f(it); it }"))
  fun onEach(f: (A) -> Unit): Try<A> = map {
    f(it)
    it
  }

  fun exists(predicate: (A) -> Boolean): Boolean = fold({ false }, { predicate(it) })

  //@Deprecated(DeprecatedUnsafeAccess, ReplaceWith("getOrElse { ifEmpty }"))
  abstract fun get(): A

  //@Deprecated(DeprecatedUnsafeAccess, ReplaceWith("map { body(it); it }"))
  fun onSuccess(body: (A) -> Unit): Try<A> {
    foreach(body)
    return this
  }

  //@Deprecated(DeprecatedUnsafeAccess, ReplaceWith("fold ({ Try { body(it); it }}, { Try.just(it) })"))
  fun onFailure(body: (Throwable) -> Unit): Try<A> = when (this) {
    is Success -> this
    is Failure -> {
      body(exception)
      this
    }
  }

  //fun toOption(): Option<A> = fold({ None }, { Some(it) })

  //fun toEither(): Either<Throwable, A> = fold({ Left(it) }, { Right(it) })

  fun <B> foldLeft(initial: B, operation: (B, A) -> B): B = fold({ initial }, { operation(initial, it) })

  //fun <B> foldRight(initial: Eval<B>, operation: (A, Eval<B>) -> Eval<B>): Eval<B> = fold({ initial }, { operation(it, initial) })

  /**
   * The `Failure` type represents a computation that result in an exception.
   */
  data class Failure(val exception: Throwable) : Try<Nothing>() {
    override fun isFailure(): Boolean = true

    override fun isSuccess(): Boolean = false

    override fun get(): Nothing {
      throw exception
    }
  }

  /**
   * The `Success` type represents a computation that return a successfully computed value.
   */
  data class Success<out A>(val value: A) : Try<A>() {
    override fun isFailure(): Boolean = false

    override fun isSuccess(): Boolean = true

    override fun get(): A = value
  }
}

sealed class TryException(override val message: String) : Exception(message) {
  data class PredicateException(override val message: String) : TryException(message)
  data class UnsupportedOperationException(override val message: String) : TryException(message)
}

/**
 * Returns the value from this `Success` or the given `default` argument if this is a `Failure`.
 *
 * ''Note:'': This will throw an exception if it is not a success and default throws an exception.
 */
inline fun <B> Try<B>.getOrDefault(default: () -> B): B = fold({ default() }, {it})

/**
 * Returns the value from this `Success` or the given `default` argument if this is a `Failure`.
 *
 * ''Note:'': This will throw an exception if it is not a success and default throws an exception.
 */
inline fun <B> Try<B>.getOrElse(default: (Throwable) -> B): B = fold(default, {it})

/**
 * Returns the value from this `Success` or null if this is a `Failure`.
 */
fun <B> Try<B>.orNull(): B? = getOrElse { null }

inline fun <B, A : B> Try<A>.orElse(f: () -> Try<B>): Try<B> = when (this) {
  is Try.Success -> this
  is Try.Failure -> f()
}

/**
 * Applies the given function `f` if this is a `Failure`, otherwise returns this if this is a `Success`.
 * This is like `flatMap` for the exception.
 */
fun <B> Try<B>.recoverWith(f: (Throwable) -> Try<B>): Try<B> = fold({ f(it) }, { Success(it) })

//@Deprecated(DeprecatedAmbiguity, ReplaceWith("recoverWith(f)"))
fun <A> Try<A>.rescue(f: (Throwable) -> Try<A>): Try<A> = recoverWith(f)

/**
 * Applies the given function `f` if this is a `Failure`, otherwise returns this if this is a `Success`.
 * This is like map for the exception.
 */
fun <B> Try<B>.recover(f: (Throwable) -> B): Try<B> = fold({ Success(f(it)) }, { Success(it) })

//@Deprecated(DeprecatedAmbiguity, ReplaceWith("recover(f)"))
fun <A> Try<A>.handle(f: (Throwable) -> A): Try<A> = recover(f)

/**
 * Completes this `Try` by applying the function `ifFailure` to this if this is of type `Failure`,
 * or conversely, by applying `ifSuccess` if this is a `Success`.
 */
//@Deprecated(DeprecatedAmbiguity, ReplaceWith("fold(ifFailure, ifSuccess)"))
inline fun <A, B> Try<A>.transform(ifSuccess: (A) -> Try<B>, ifFailure: (Throwable) -> Try<B>): Try<B> = fold({ ifFailure(it) }, { flatMap(ifSuccess) })

fun <A> (() -> A).try_(): Try<A> = Try(this)

fun <T> Try<Try<T>>.flatten(): Try<T> = flatMap({it})
