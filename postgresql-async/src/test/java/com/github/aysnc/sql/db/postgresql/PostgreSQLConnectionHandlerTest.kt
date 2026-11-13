package com.github.aysnc.sql.db.postgresql

import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.postgresql.codec.PostgreSQLConnectionDelegate
import com.github.jasync.sql.db.postgresql.codec.PostgreSQLConnectionHandler
import com.github.jasync.sql.db.postgresql.column.PostgreSQLColumnEncoderRegistry
import io.mockk.mockk
import io.mockk.verify
import io.netty.channel.ChannelHandlerContext
import org.junit.Test

class PostgreSQLConnectionHandlerTest {

    @Test
    fun `channelInactive should notify delegate about disconnect`() {
        val delegate = mockk<PostgreSQLConnectionDelegate>(relaxed = true)
        val configuration = Configuration("test")
        val handler = PostgreSQLConnectionHandler(
            configuration,
            PostgreSQLColumnEncoderRegistry.Instance,
            delegate,
            configuration.eventLoopGroup,
            configuration.executionContext
        )
        val ctx = mockk<ChannelHandlerContext>(relaxed = true)

        handler.channelInactive(ctx)

        verify { delegate.onDisconnect() }
    }
}
