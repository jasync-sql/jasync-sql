package com.github.jasync.sql.db.util

import io.netty.channel.EventLoopGroup
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.util.internal.logging.InternalLoggerFactory
import io.netty.util.internal.logging.Slf4JLoggerFactory
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

object NettyUtils {

    init {
        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE)
        if (Epoll.isAvailable()) {
            logger.info { "selected transport - native (epoll)" }
        } else {
            logger.info { "selected transport - nio" }
        }
    }

    val DefaultEventLoopGroup: EventLoopGroup by lazy {
        if (Epoll.isAvailable()) {
            EpollEventLoopGroup(0, DaemonThreadsFactory("db-sql-netty"))
        } else {
            NioEventLoopGroup(0, DaemonThreadsFactory("db-sql-netty"))
        }
    }
}
