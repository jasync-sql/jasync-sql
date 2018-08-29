package com.github.jasync.sql.db.util

/*
 * Copyright 2013 Maurício Linhares
 *
 * Maurício Linhares licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

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
