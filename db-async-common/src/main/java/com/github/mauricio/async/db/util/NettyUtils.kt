package com.github.mauricio.async.db.util

import io.netty.channel.nio.NioEventLoopGroup
import io.netty.util.internal.logging.InternalLoggerFactory
import io.netty.util.internal.logging.Slf4JLoggerFactory

object NettyUtils {

  init {
    InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE)
    //TODO lazy
    val DefaultEventLoopGroup = NioEventLoopGroup(0, DaemonThreadsFactory("db-async-netty"))
  }
}
