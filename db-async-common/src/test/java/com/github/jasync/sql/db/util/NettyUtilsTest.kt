package com.github.jasync.sql.db.util

import com.github.jasync.sql.db.Configuration
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class NettyUtilsTest {

    @Test
    fun getDefaultEventLoopGroup() {
        assertThat(NettyUtils.DefaultEventLoopGroup.javaClass.simpleName)
            .isEqualTo("NioEventLoopGroup")
    }

    @Test
    fun getSocketChannelClass() {
        assertThat(NettyUtils.getSocketChannelClass(NettyUtils.DefaultEventLoopGroup).simpleName)
            .isEqualTo("NioSocketChannel")
    }

    @Test
    fun getSocketChannelClassAndSocketAddress() {
        val socketChannelAddress = NettyUtils.getSocketChannelClassAndSocketAddress(
            NettyUtils.DefaultEventLoopGroup,
            Configuration(username = "root", socketPath = "/tmp/mysql.sock")
        )
        assertThat(socketChannelAddress.socketChannelClass.simpleName).isEqualTo("NioSocketChannel")
        assertThat(socketChannelAddress.socketAddress::class.simpleName).isEqualTo("InetSocketAddress")
    }
}
