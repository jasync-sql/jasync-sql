package com.github.jasync.sql.db.util

import io.netty.channel.nio.NioEventLoopGroup
import io.netty.util.internal.logging.InternalLoggerFactory
import io.netty.util.internal.logging.Slf4JLoggerFactory

object NettyUtils {

    init {
        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE)

    }

    val DefaultEventLoopGroup: NioEventLoopGroup by lazy {
        NioEventLoopGroup(0, DaemonThreadsFactory("db-sql-netty"))
    }
}
