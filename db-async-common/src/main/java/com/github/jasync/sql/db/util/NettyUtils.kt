package com.github.jasync.sql.db.util

import io.netty.channel.Channel
import io.netty.channel.EventLoopGroup
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollDomainSocketChannel
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollSocketChannel
import io.netty.channel.kqueue.KQueue
import io.netty.channel.kqueue.KQueueDomainSocketChannel
import io.netty.channel.kqueue.KQueueEventLoopGroup
import io.netty.channel.kqueue.KQueueSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.util.internal.logging.InternalLoggerFactory
import io.netty.util.internal.logging.Slf4JLoggerFactory
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun EventLoopGroup.domainSocketCompatible() = this is KQueueEventLoopGroup || this is EpollEventLoopGroup

object NettyUtils {

    init {
        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE)
        when {
            tryOrFalse { Epoll.isAvailable() } -> logger.info { "jasync available transport - native (epoll)" }
            tryOrFalse { KQueue.isAvailable() } -> logger.info { "jasync available transport - native (kqueue)" }
            else -> logger.info { "jasync selected transport - nio" }
        }
    }

    val DefaultEventLoopGroup: EventLoopGroup by lazy {
        when {
            tryOrFalse { Epoll.isAvailable() } -> EpollEventLoopGroup(0, DaemonThreadsFactory("db-sql-netty"))
            tryOrFalse { KQueue.isAvailable() } -> KQueueEventLoopGroup(0, DaemonThreadsFactory("db-sql-netty"))
            else -> NioEventLoopGroup(0, DaemonThreadsFactory("db-sql-netty"))
        }
    }

    fun getSocketChannelClass(eventLoopGroup: EventLoopGroup, useDomainSocket: Boolean = false): Class<out Channel> = when {
        tryOrFalse { eventLoopGroup is EpollEventLoopGroup } -> if (useDomainSocket) EpollDomainSocketChannel::class.java else EpollSocketChannel::class.java
        tryOrFalse { eventLoopGroup is KQueueEventLoopGroup } -> if (useDomainSocket) KQueueDomainSocketChannel::class.java else KQueueSocketChannel::class.java
        else -> {
            logger.info { "domain socket is not supported by NioEventLoopGroup, useDomainSocket flag is skipped" }
            NioSocketChannel::class.java
        }
    }

    private fun tryOrFalse(fn: () -> Boolean): Boolean {
        return try {
            fn.invoke()
        } catch (t: Throwable) {
            false
        }
    }
}
