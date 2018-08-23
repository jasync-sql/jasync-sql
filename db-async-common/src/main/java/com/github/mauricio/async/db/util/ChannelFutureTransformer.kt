package com.github.mauricio.async.db.util

import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelFuture
//import scala.concurrent.Promise
//import scala.concurrent.Future
import com.github.mauricio.async.db.exceptions.CanceledChannelFutureException
//import scala.language.implicitConversions
import java.util.concurrent.CompletableFuture

object ChannelFutureTransformer {

  fun toFuture(channelFuture: ChannelFuture): CompletableFuture<ChannelFuture> {
    val promise = CompletableFuture<ChannelFuture>()

    val listener = ChannelFutureListener { future ->
      if (future.isSuccess()) {
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
    channelFuture.addListener(listener)

    return promise
  }

}
