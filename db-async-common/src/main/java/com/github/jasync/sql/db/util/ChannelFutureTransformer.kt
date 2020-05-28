package com.github.jasync.sql.db.util

import com.github.jasync.sql.db.exceptions.CanceledChannelFutureException
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

fun ChannelFuture.toCompletableFuture(): CompletableFuture<ChannelFuture> {
    val promise = CompletableFuture<ChannelFuture>()

    installOnChannelFuture(promise)

    return promise
}

fun ChannelFuture.installOnChannelFuture(promise: CompletableFuture<ChannelFuture>) {
    val listener = ChannelFutureListener { future ->
        if (future.isSuccess) {
            promise.complete(future)
        } else {
            val exception = if (future.cause() == null) {
                CanceledChannelFutureException(future)
            } else {
                future.cause()
            }
            promise.completeExceptionally(exception)
        }
    }
    this.addListener(listener)
}
fun ChannelFuture.installOnFuture(promise: CompletableFuture<Channel>) {
    val listener = ChannelFutureListener { future ->
        if (future.isSuccess) {
            promise.complete(future.channel())
        } else {
            val exception = if (future.cause() == null) {
                CanceledChannelFutureException(future)
            } else {
                future.cause()
            }
            promise.completeExceptionally(exception)
        }
    }
    this.addListener(listener)
}

fun ChannelFuture.onFailure(executor: Executor, handler: (Throwable) -> Unit) {
    this.toCompletableFuture().onFailureAsync(executor = executor, onFailureFun = handler)
}
