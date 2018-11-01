package com.github.jasync.sql.db.util

//import scala.concurrent.Promise
//import scala.concurrent.Future
//import scala.language.implicitConversions
import com.github.jasync.sql.db.exceptions.CanceledChannelFutureException
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

fun ChannelFuture.toCompletableFuture(): CompletableFuture<ChannelFuture> {
  val promise = CompletableFuture<ChannelFuture>()

  val listener = ChannelFutureListener { future ->
    if (future.isSuccess) {
      promise.complete(future)
    } else {
      val exception = if (future.cause() == null) {
        CanceledChannelFutureException(future)
            .fillInStackTrace()
      } else {
        future.cause()
      }
      promise.completeExceptionally(exception)
    }
  }
  this.addListener(listener)

  return promise
}


fun ChannelFuture.onFailure(executor: Executor, handler: (Throwable) -> Unit) {
  this.toCompletableFuture().onFailureAsync(executor = executor, onFailureFun = handler)
}
