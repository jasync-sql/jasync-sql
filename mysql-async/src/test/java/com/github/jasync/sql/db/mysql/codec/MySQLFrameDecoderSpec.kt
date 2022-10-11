package com.github.jasync.sql.db.mysql.codec

import com.github.jasync.sql.db.mysql.column.ColumnTypes
import com.github.jasync.sql.db.mysql.message.server.ColumnDefinitionMessage
import com.github.jasync.sql.db.mysql.message.server.ColumnProcessingFinishedMessage
import com.github.jasync.sql.db.mysql.message.server.ErrorMessage
import com.github.jasync.sql.db.mysql.message.server.OkMessage
import com.github.jasync.sql.db.mysql.message.server.ResultSetRowMessage
import com.github.jasync.sql.db.util.ByteBufferUtils
import com.github.jasync.sql.db.util.ChannelWrapper
import com.github.jasync.sql.db.util.writeLength
import com.github.jasync.sql.db.util.writeLengthEncodedString
import io.netty.buffer.ByteBuf
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.util.CharsetUtil
import org.junit.Test
import java.nio.charset.Charset
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MySQLFrameDecoderSpec {

    private val charset: Charset = CharsetUtil.UTF_8

    @Test
    fun `decode an OK message correctly`() {
        val buffer = createOkPacket()
        val decoder = this.createPipeline()
        decoder.writeInbound(buffer)

        val ok = decoder.readInbound() as OkMessage
        assertEquals(10, ok.affectedRows)
        assertEquals(15, ok.lastInsertId)
        assertEquals("this is a test", ok.message)
        assertEquals(5, ok.statusFlags)
        assertEquals(6, ok.warnings)
    }

    @Test
    fun `decode an error message`() {
        val content = "this is the error message"
        val buffer = createErrorPacket(content)
        val decoder = createPipeline()

        decoder.writeInbound(buffer)

        val error = decoder.readInbound() as ErrorMessage
        assertEquals(27, error.errorCode)
        assertEquals(content, error.errorMessage)
        assertEquals("HZAWAY", error.sqlState)
    }

    @Test
    fun `on a query process it should correctly send an OK`() {
        val decoder = MySQLFrameDecoder(charset, "[mysql-connection]")
        decoder.hasDoneHandshake = true
        val embedder = EmbeddedChannel(decoder)
        embedder.config().allocator = LittleEndianByteBufAllocator.INSTANCE

        decoder.queryProcessStarted()

        assertTrue(decoder.isInQuery)
        assertTrue(decoder.processingColumns)

        val buffer = createOkPacket()
        assertTrue(embedder.writeInbound(buffer))
        assertEquals("this is a test", (embedder.readInbound() as OkMessage).message)

        assertFalse(decoder.isInQuery)
        assertFalse(decoder.processingColumns)
    }

    @Test
    fun `on query process it should correctly send an error`() {

        val decoder = MySQLFrameDecoder(charset, "[mysql-connection]")
        decoder.hasDoneHandshake = true
        val embedder = EmbeddedChannel(decoder)
        embedder.config().allocator = LittleEndianByteBufAllocator.INSTANCE

        decoder.queryProcessStarted()

        assertTrue(decoder.isInQuery)
        assertTrue(decoder.processingColumns)

        val content = "this is a crazy error"

        val buffer = createErrorPacket(content)

        assertTrue(embedder.writeInbound(buffer))
        assertEquals(content, (embedder.readInbound() as ErrorMessage).errorMessage)
        assertFalse(decoder.isInQuery)
        assertFalse(decoder.processingColumns)
    }

    @Test
    fun `on query process it should correctly handle a result set`() {

        val decoder = MySQLFrameDecoder(charset, "[mysql-connection]")
        decoder.hasDoneHandshake = true
        val embedder = EmbeddedChannel(decoder)
        embedder.config().allocator = LittleEndianByteBufAllocator.INSTANCE

        decoder.queryProcessStarted()

        assertEquals(0L, decoder.totalColumns)

        val columnCountBuffer = ByteBufferUtils.packetBuffer()
        columnCountBuffer.writeLength(2)
        ChannelWrapper.writePacketLength(columnCountBuffer)

        embedder.writeInbound(columnCountBuffer)

        assertEquals(2, decoder.totalColumns)

        val columnId = createColumnPacket("id", ColumnTypes.FIELD_TYPE_LONG)
        val columnName = createColumnPacket("name", ColumnTypes.FIELD_TYPE_VARCHAR)

        embedder.writeInbound(columnId)
        assertEquals("id", (embedder.readInbound() as ColumnDefinitionMessage).name)
        assertEquals(1, decoder.processedColumns)

        embedder.writeInbound(columnName)

        assertEquals("name", (embedder.readInbound() as ColumnDefinitionMessage).name)

        assertEquals(2, decoder.processedColumns)

        embedder.writeInbound(this.createEOFPacket())

        assertEquals(8765, (embedder.readInbound() as ColumnProcessingFinishedMessage).eofMessage.flags)

        assertFalse(decoder.processingColumns)

        val row = ByteBufferUtils.packetBuffer()
        row.writeLengthEncodedString("1", charset)
        row.writeLengthEncodedString("some name", charset)
        ChannelWrapper.writePacketLength(row, 0)

        embedder.writeInbound(row)

        embedder.readInbound() as ResultSetRowMessage

        embedder.writeInbound(this.createEOFPacket())

        assertFalse(decoder.isInQuery)
    }

    fun createPipeline(): EmbeddedChannel {
        val decoder = MySQLFrameDecoder(charset, "[mysql-connection]")
        decoder.hasDoneHandshake = true
        val channel = EmbeddedChannel(decoder)
        channel.config().allocator = LittleEndianByteBufAllocator.INSTANCE
        return channel
    }

    fun createOkPacket(): ByteBuf {
        val buffer = ByteBufferUtils.packetBuffer()
        buffer.writeByte(0)
        buffer.writeLength(10)
        buffer.writeLength(15)
        buffer.writeShort(5)
        buffer.writeShort(6)
        buffer.writeBytes("this is a test".toByteArray())
        ChannelWrapper.writePacketLength(buffer, 0)
        return buffer
    }

    fun createErrorPacket(content: String): ByteBuf {
        val buffer = ByteBufferUtils.packetBuffer()
        buffer.writeByte(0xff)
        buffer.writeShort(27)
        buffer.writeByte('H'.toByte().toInt())
        buffer.writeBytes("ZAWAY".toByteArray(charset))
        buffer.writeBytes(content.toByteArray(charset))
        ChannelWrapper.writePacketLength(buffer, 0)
        return buffer
    }

    fun createColumnPacket(name: String, columnType: Int): ByteBuf {
        val buffer = assertNotNull(ByteBufferUtils.packetBuffer())
        buffer.writeLengthEncodedString("def", charset)
        buffer.writeLengthEncodedString("some_schema", charset)
        buffer.writeLengthEncodedString("some_table", charset)
        buffer.writeLengthEncodedString("some_table", charset)
        buffer.writeLengthEncodedString(name, charset)
        buffer.writeLengthEncodedString(name, charset)
        ChannelWrapper.writeLength(buffer, 12)
        buffer.writeShort(0x03)
        buffer.writeInt(10)
        buffer.writeByte(columnType)
        buffer.writeShort(76)
        buffer.writeByte(0)
        buffer.writeShort(56)
        ChannelWrapper.writePacketLength(buffer, 0)
        return buffer
    }

    fun createEOFPacket(): ByteBuf {
        val buffer = ByteBufferUtils.packetBuffer()
        buffer.writeByte(0xfe)
        buffer.writeShort(879)
        buffer.writeShort(8765)

        ByteBufferUtils.writePacketLength(buffer, 0)

        return buffer
    }
}
