package com.github.jasync.sql.db.util

import io.netty.buffer.Unpooled
import io.netty.util.CharsetUtil
import org.junit.Test
import kotlin.test.assertEquals

class ChannelUtilsSpec {

    private val charset = CharsetUtil.UTF_8

    @Test
    fun `correctly write and read a string`() {
        val content = "some text"
        val buffer = Unpooled.buffer()

        ByteBufferUtils.writeCString(content, buffer, charset)

        assertEquals(content, ByteBufferUtils.readCString(buffer, charset))
        assertEquals(0, buffer.readableBytes())
    }

    @Test
    fun `correctly read the buggy MySQL EOF string when there is an EOF`() {
        val content = "some text"
        val buffer = Unpooled.buffer()

        ByteBufferUtils.writeCString(content, buffer, charset)

        assertEquals(content, ByteBufferUtils.readUntilEOF(buffer, charset))
        assertEquals(0, buffer.readableBytes())
    }

    @Test
    fun `correctly read the buggy MySQL EOF string when there is no EOF`() {
        val content = "some text"
        val buffer = Unpooled.buffer()

        buffer.writeBytes(content.toByteArray(charset))

        assertEquals(content, ByteBufferUtils.readUntilEOF(buffer, charset))
        assertEquals(0, buffer.readableBytes())
    }
}
