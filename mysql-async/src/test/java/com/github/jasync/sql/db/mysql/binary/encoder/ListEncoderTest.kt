package com.github.jasync.sql.db.mysql.binary.encoder

import io.netty.buffer.Unpooled
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals
import kotlin.test.fail

class ListEncoderTest {

    private val charset = StandardCharsets.UTF_8
    private val encoder = ListEncoder(charset)

    @Test
    fun `encodesTo should return VAR_STRING type`() {
        assertEquals(com.github.jasync.sql.db.mysql.column.ColumnTypes.FIELD_TYPE_VAR_STRING, encoder.encodesTo())
    }

    @Test
    fun `encode should write empty list as length-encoded empty string`() {
        val buffer = Unpooled.buffer()
        val list = emptyList<String>()
        encoder.encode(list, buffer)

        assertEquals(0.toByte(), buffer.readByte()) // Length of empty string is 0
        assertEquals(0, buffer.readableBytes())
    }

    @Test
    fun `encode should write single item list`() {
        val buffer = Unpooled.buffer()
        val list = listOf("hello")
        encoder.encode(list, buffer)

        val expectedString = "hello"
        val expectedBytes = expectedString.toByteArray(charset)

        assertEquals(expectedBytes.size.toByte(), buffer.readByte())
        val writtenBytes = ByteArray(buffer.readableBytes())
        buffer.readBytes(writtenBytes)
        assertThat(writtenBytes).isEqualTo(expectedBytes)
    }

    @Test
    fun `encode should write multiple item list as comma-separated string`() {
        val buffer = Unpooled.buffer()
        val list = listOf("hello", "world", "test")
        encoder.encode(list, buffer)

        val expectedString = "hello,world,test"
        val expectedBytes = expectedString.toByteArray(charset)

        assertEquals(expectedBytes.size.toByte(), buffer.readByte())
        val writtenBytes = ByteArray(buffer.readableBytes())
        buffer.readBytes(writtenBytes)
        assertThat(writtenBytes).isEqualTo(expectedBytes)
    }

    @Test
    fun `encode should filter out null values from list`() {
        val buffer = Unpooled.buffer()
        val list = listOf("one", null, "two", null, "three")
        encoder.encode(list, buffer)

        val expectedString = "one,two,three"
        val expectedBytes = expectedString.toByteArray(charset)

        assertEquals(expectedBytes.size.toByte(), buffer.readByte())
        val writtenBytes = ByteArray(buffer.readableBytes())
        buffer.readBytes(writtenBytes)
        assertThat(writtenBytes).isEqualTo(expectedBytes)
    }

    @Test
    fun `encode should handle list with only null values as empty list`() {
        val buffer = Unpooled.buffer()
        val list = listOf(null, null, null)
        encoder.encode(list, buffer)

        assertEquals(0.toByte(), buffer.readByte()) // Length of empty string is 0
        assertEquals(0, buffer.readableBytes())
    }

    @Test
    fun `encode should handle list of numbers`() {
        val buffer = Unpooled.buffer()
        val list = listOf(1, 20, 300)
        encoder.encode(list, buffer)

        val expectedString = "1,20,300"
        val expectedBytes = expectedString.toByteArray(charset)

        assertEquals(expectedBytes.size.toByte(), buffer.readByte())
        val writtenBytes = ByteArray(buffer.readableBytes())
        buffer.readBytes(writtenBytes)
        assertThat(writtenBytes).isEqualTo(expectedBytes)
    }

    @Test
    fun `encode should throw IllegalArgumentException for non-list value`() {
        val buffer = Unpooled.buffer()
        try {
            encoder.encode("not a list", buffer)
            fail("Expected IllegalArgumentException but no exception was thrown")
        } catch (e: IllegalArgumentException) {
            // Expected exception
        }
    }

    @Test
    fun `encode should handle string length less than 251`() {
        val buffer = Unpooled.buffer()
        val str = "a".repeat(250)
        val list = listOf(str)
        encoder.encode(list, buffer)

        val expectedBytes = str.toByteArray(charset)
        assertEquals(expectedBytes.size.toByte(), buffer.readByte()) // Length byte
        val writtenBytes = ByteArray(buffer.readableBytes())
        buffer.readBytes(writtenBytes)
        assertThat(writtenBytes).isEqualTo(expectedBytes)
    }

    @Test
    fun `encode should handle string length equal to 251`() {
        val buffer = Unpooled.buffer()
        val str = "a".repeat(251)
        val list = listOf(str)
        encoder.encode(list, buffer)

        val expectedBytes = str.toByteArray(charset)
        assertEquals(252.toByte(), buffer.readByte()) // Prefix for 2-byte length
        assertEquals(expectedBytes.size.toShort(), buffer.readShortLE())
        val writtenBytes = ByteArray(buffer.readableBytes())
        buffer.readBytes(writtenBytes)
        assertThat(writtenBytes).isEqualTo(expectedBytes)
    }

    @Test
    fun `encode should handle string length between 251 and 65535`() {
        val buffer = Unpooled.buffer()
        val str = "a".repeat(300)
        val list = listOf(str)
        encoder.encode(list, buffer)

        val expectedBytes = str.toByteArray(charset)
        assertEquals(252.toByte(), buffer.readByte()) // Prefix for 2-byte length
        assertEquals(expectedBytes.size.toShort(), buffer.readShortLE())
        val writtenBytes = ByteArray(buffer.readableBytes())
        buffer.readBytes(writtenBytes)
        assertThat(writtenBytes).isEqualTo(expectedBytes)
    }

    @Test
    fun `encode should handle string length equal to 65536`() {
        val buffer = Unpooled.buffer()
        val str = "a".repeat(65536)
        val list = listOf(str)
        encoder.encode(list, buffer)

        val expectedBytes = str.toByteArray(charset)
        assertEquals(253.toByte(), buffer.readByte()) // Prefix for 3-byte length
        assertEquals(expectedBytes.size, readUnsignedMediumLE(buffer))
        val writtenBytes = ByteArray(buffer.readableBytes())
        buffer.readBytes(writtenBytes)
        assertThat(writtenBytes).isEqualTo(expectedBytes)
    }

    // Helper to read unsigned medium for testing
    private fun readUnsignedMediumLE(buffer: io.netty.buffer.ByteBuf): Int {
        val b1 = buffer.readUnsignedByte().toInt()
        val b2 = buffer.readUnsignedByte().toInt()
        val b3 = buffer.readUnsignedByte().toInt()
        return b1 or (b2 shl 8) or (b3 shl 16)
    }
}
