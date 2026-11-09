package com.github.jasync.sql.db.mysql.codec

import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.column.ColumnDecoder
import com.github.jasync.sql.db.general.ColumnData
import com.github.jasync.sql.db.general.MutableResultSet
import com.github.jasync.sql.db.mysql.binary.decoder.ByteArrayDecoder
import com.github.jasync.sql.db.mysql.column.ColumnTypes
import com.github.jasync.sql.db.mysql.message.server.ColumnDefinitionMessage
import com.github.jasync.sql.db.mysql.message.server.ResultSetRowMessage
import com.github.jasync.sql.db.mysql.util.CharsetMapper
import io.mockk.mockk
import io.netty.buffer.ByteBuf
import io.netty.buffer.PooledByteBufAllocator
import io.netty.buffer.Unpooled
import io.netty.buffer.UnpooledByteBufAllocator
import io.netty.channel.DefaultEventLoopGroup
import io.netty.channel.embedded.EmbeddedChannel
import org.junit.Test
import java.nio.ByteOrder
import java.nio.charset.Charset
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LittleEndianByteBufAllocatorSpec {

    @Test
    @Suppress("DEPRECATION")
    fun `defaults to PooledByteBufAllocator and allocates little endian buffers`() {
        val allocator = LittleEndianByteBufAllocator()
        assertEquals(PooledByteBufAllocator.DEFAULT.isDirectBufferPooled, allocator.isDirectBufferPooled)

        val heapBuf = allocator.heapBuffer(16)
        val directBuf = allocator.directBuffer(16)
        val ioBuf = allocator.ioBuffer(16)
        try {
            assertEquals(ByteOrder.LITTLE_ENDIAN, heapBuf.order())
            assertEquals(ByteOrder.LITTLE_ENDIAN, directBuf.order())
            assertEquals(ByteOrder.LITTLE_ENDIAN, ioBuf.order())
        } finally {
            heapBuf.release()
            directBuf.release()
            ioBuf.release()
        }
    }

    @Test
    fun `delegates isDirectBufferPooled to custom allocator`() {
        val unpooled = LittleEndianByteBufAllocator(UnpooledByteBufAllocator(false))
        assertFalse(unpooled.isDirectBufferPooled)
    }

    @Test
    fun `releases all ResultSetRowMessage column slices when a column decoder throws`() {
        val failingDecoder = object : ColumnDecoder {
            override fun decode(kind: ColumnData, value: ByteBuf, charset: Charset): Any {
                throw IllegalArgumentException("decode failure on col 0")
            }

            override fun decode(value: String): Any {
                throw IllegalArgumentException("decode failure")
            }
        }
        val col0 = ColumnDefinitionMessage(
            "bad", "db", "t", "t", "c0", "c0",
            CharsetMapper.Binary, 10, ColumnTypes.FIELD_TYPE_VARCHAR, 0, 0,
            ByteArrayDecoder, failingDecoder
        )
        val col1 = ColumnDefinitionMessage(
            "ok", "db", "t", "t", "c1", "c1",
            CharsetMapper.Binary, 10, ColumnTypes.FIELD_TYPE_VARCHAR, 0, 0,
            ByteArrayDecoder, failingDecoder
        )

        val group = DefaultEventLoopGroup(1)
        try {
            var capturedException: Throwable? = null
            val delegate = object : MySQLHandlerDelegate by mockk(relaxed = true) {
                override fun exceptionCaught(exception: Throwable) {
                    capturedException = exception
                }
            }
            val handler = MySQLConnectionHandler(
                Configuration("root"),
                CharsetMapper.Instance,
                delegate,
                group,
                connectionId = "test-conn"
            )
            val currentQueryField = MySQLConnectionHandler::class.java.getDeclaredField("currentQuery").apply {
                isAccessible = true
            }
            currentQueryField.set(handler, MutableResultSet(listOf(col0, col1)))

            val channel = EmbeddedChannel(handler)
            val slice0 = Unpooled.copiedBuffer("abc", Charsets.UTF_8)
            val slice1 = Unpooled.copiedBuffer("def", Charsets.UTF_8)
            val row = ResultSetRowMessage().apply {
                add(slice0)
                add(slice1)
            }

            channel.writeInbound(row)

            assertNotNull(capturedException)
            assertTrue(capturedException is IllegalArgumentException)
            assertEquals(0, slice0.refCnt())
            assertEquals(0, slice1.refCnt())
            channel.finishAndReleaseAll()
        } finally {
            group.shutdownGracefully().sync()
        }
    }
}
