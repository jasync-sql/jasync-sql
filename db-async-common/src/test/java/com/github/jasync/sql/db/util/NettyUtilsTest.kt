package com.github.jasync.sql.db.util

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
}
