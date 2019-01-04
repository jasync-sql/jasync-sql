package com.github.jasync.sql.db.util

import io.netty.channel.EventLoopGroup
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollSocketChannel
import io.netty.channel.kqueue.KQueue
import io.netty.channel.kqueue.KQueueEventLoopGroup
import io.netty.channel.kqueue.KQueueSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.util.internal.logging.InternalLoggerFactory
import io.netty.util.internal.logging.Slf4JLoggerFactory
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

object NettyUtils {

    init {
        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE)
        when {
            Epoll.isAvailable() -> logger.info { "jasync selected transport - native (epoll)" }
            KQueue.isAvailable() -> logger.info { "jasync selected transport - native (kqueue)" }
            else -> logger.info { "jasync selected transport - nio" }
        }
    }

    val DefaultEventLoopGroup: EventLoopGroup by lazy {
        when {
            Epoll.isAvailable() -> EpollEventLoopGroup(0, DaemonThreadsFactory("db-sql-netty"))
            KQueue.isAvailable() -> KQueueEventLoopGroup(0, DaemonThreadsFactory("db-sql-netty"))
            else -> NioEventLoopGroup(0, DaemonThreadsFactory("db-sql-netty"))
        }
    }

    fun getSocketChannelClass(eventLoopGroup: EventLoopGroup): Class<out SocketChannel> = when {
        eventLoopGroup is EpollEventLoopGroup -> EpollSocketChannel::class.java
        eventLoopGroup is KQueueEventLoopGroup -> KQueueSocketChannel::class.java
        else -> NioSocketChannel::class.java
    }
}
