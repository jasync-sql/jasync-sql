package com.github.jasync.sql.db.util

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.ForkJoinPool
import java.util.function.BiConsumer
import java.util.function.Function


inline fun <A, B> CompletableFuture<A>.map(executor: Executor = ForkJoinExecutor, crossinline f: (A) -> B): CompletableFuture<B> =
    thenApplyAsync(Function { f(it) }, executor)

inline fun <A, B> CompletableFuture<A>.flatMap(executor: Executor = ForkJoinExecutor, crossinline f: (A) -> CompletableFuture<B>): CompletableFuture<B> =
    thenComposeAsync(Function { f(it) }, executor)

fun <A> CompletableFuture<CompletableFuture<A>>.flatten(): CompletableFuture<A> = flatMap { it }

inline fun <A> CompletableFuture<A>.filter(executor: Executor = ForkJoinExecutor, crossinline predicate: (A) -> Boolean): CompletableFuture<A> =
    map(executor) {
      if (predicate(it)) it else throw NoSuchElementException("CompletableFuture.filter predicate is not satisfied")
    }


inline fun <A> CompletableFuture<A>.onFailure(executor: Executor = ForkJoinExecutor, crossinline onFailureFun: (Throwable) -> Unit): CompletableFuture<A> =
    whenCompleteAsync(BiConsumer { _, t -> if (t != null) onFailureFun(t) }, executor)

inline fun <A> CompletableFuture<A>.onComplete(executor: Executor = ForkJoinExecutor, crossinline onCompleteFun: (A, Throwable?) -> Unit): CompletableFuture<A> =
    whenCompleteAsync(BiConsumer { a, t -> onCompleteFun(a, t) }, executor)

inline fun <A> CompletableFuture<A>.onComplete(executor: Executor = ForkJoinExecutor, crossinline onCompleteFun: (Try<A>) -> Unit): CompletableFuture<A> =
    whenCompleteAsync(BiConsumer { a, t -> onCompleteFun(if (t != null) Try.raise(t) else Try.just(a)) }, executor)

fun <A> CompletableFuture<A>.success(a: A): CompletableFuture<A> = this.also { it.complete(a) }

fun <A> CompletableFuture<A>.tryFailure(e: Throwable): Boolean = this.completeExceptionally(e)
fun <A> CompletableFuture<A>.failure(e: Throwable): Boolean = this.completeExceptionally(e)
fun <A> CompletableFuture<A>.failed(e: Throwable): CompletableFuture<A> = this.also { it.completeExceptionally(e) }


fun <A> CompletableFuture<A>.complete(t: Try<A>) = when (t) {
  is Success -> this.complete(t.value)
  is Failure -> this.completeExceptionally(t.exception)
}

val <A> CompletableFuture<A>.isCompleted get() = this.isDone

object FuturePromise {
  fun <A> successful(a: A): CompletableFuture<A> = CompletableFuture<A>().also { it.complete(a) }
}

object DirectExecutor : Executor {
  override fun execute(command: Runnable) {
    command.run()
  }
}

object ForkJoinExecutor : ExecutorService by ForkJoinPool.commonPool()
