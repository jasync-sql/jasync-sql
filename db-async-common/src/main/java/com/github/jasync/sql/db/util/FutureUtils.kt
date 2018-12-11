package com.github.jasync.sql.db.util

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.function.BiConsumer
import java.util.function.Function


object FP {
    fun <A> successful(a: A): CompletableFuture<A> = CompletableFuture<A>().also { it.complete(a) }
    fun <A> failed(t: Throwable): CompletableFuture<A> = CompletableFuture<A>().failed(t)
}

fun <A> CompletableFuture<A>.success(a: A): CompletableFuture<A> = this.also { it.complete(a) }
fun <A> CompletableFuture<A>.failed(e: Throwable): CompletableFuture<A> = this.also { it.completeExceptionally(e) }
fun <A> CompletableFuture<A>.tryFailure(e: Throwable): Boolean = this.completeExceptionally(e)

fun <A> Try<A>.asCompletedFuture(): CompletableFuture<A> = when (this) {
    is Success -> FP.successful(this.value)
    is Failure -> FP.failed(this.exception)
}

fun <A> CompletableFuture<A>.getAsTry(millis: Long, unit: TimeUnit): Try<A> = Try { get(millis, unit) }

inline fun <A, B> CompletableFuture<A>.mapTry(crossinline f: (A, Throwable?) -> B): CompletableFuture<B> =
    handle { a, t: Throwable? -> f(a, t) }

inline fun <A, B> CompletableFuture<A>.map(crossinline f: (A) -> B): CompletableFuture<B> =
    thenApply { f(it) }

inline fun <A, B> CompletableFuture<A>.mapAsync(executor: Executor, crossinline f: (A) -> B): CompletableFuture<B> =
    thenApplyAsync(Function { f(it) }, executor)

inline fun <A, B> CompletableFuture<A>.flatMapAsync(
    executor: Executor,
    crossinline f: (A) -> CompletableFuture<B>
): CompletableFuture<B> =
    thenComposeAsync(Function { f(it) }, executor)

inline fun <A> CompletableFuture<A>.onFailureAsync(
    executor: Executor,
    crossinline onFailureFun: (Throwable) -> Unit
): CompletableFuture<A> =
    whenCompleteAsync(BiConsumer { _, t -> if (t != null) onFailureFun(t) }, executor)

inline fun <A> CompletableFuture<A>.onComplete(crossinline onCompleteFun: (Try<A>) -> Unit): CompletableFuture<A> =
    whenComplete { a, t -> onCompleteFun(if (t != null) Try.raise(t) else Try.just(a)) }

inline fun <A> CompletableFuture<A>.onCompleteAsync(
    executor: Executor,
    crossinline onCompleteFun: (Try<A>) -> Unit
): CompletableFuture<A> =
    whenCompleteAsync(BiConsumer { a, t -> onCompleteFun(if (t != null) Try.raise(t) else Try.just(a)) }, executor)

val <A> CompletableFuture<A>.isCompleted get() = this.isDone
val <A> CompletableFuture<A>.isSuccess: Boolean get() = this.isDone && this.isCompleted
val <A> CompletableFuture<A>.isFailure: Boolean get() = this.isDone && this.isCompletedExceptionally


fun <A> CompletableFuture<A>.complete(t: Try<A>) = when (t) {
    is Success -> this.complete(t.value)
    is Failure -> this.completeExceptionally(t.exception)
}



