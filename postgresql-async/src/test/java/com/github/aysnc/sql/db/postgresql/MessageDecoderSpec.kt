package com.github.aysnc.sql.db.postgresql

import com.github.jasync.sql.db.exceptions.NegativeMessageSizeException
import com.github.jasync.sql.db.postgresql.codec.MessageDecoder
import com.github.jasync.sql.db.postgresql.codec.MessageDecoder_DefaultMaximumSize
import com.github.jasync.sql.db.postgresql.exceptions.MessageTooLongException
import com.github.jasync.sql.db.postgresql.messages.backend.ErrorMessage
import com.github.jasync.sql.db.postgresql.messages.backend.ServerMessage
import com.github.jasync.sql.db.util.length
import io.mockk.mockk
import io.netty.buffer.Unpooled
import io.netty.util.CharsetUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test


class MessageDecoderSpec {

    val decoder = MessageDecoder(false, CharsetUtil.UTF_8)


    @Test
    fun `"message decoder" should "not try to decode if there is not enought data available"`() {

        val buffer = Unpooled.buffer()

        buffer.writeByte('R'.toInt())
        buffer.writeByte(1)
        buffer.writeByte(2)
        val out = mutableListOf<Any>()

        this.decoder.decode(mockk(), buffer, out)
        assertThat(out).isEmpty()
    }

    @Test
    fun `"should not try to decode if there is a type and lenght but it's not long enough"`() {

        val buffer = Unpooled.buffer()

        buffer.writeByte('R'.toInt())
        buffer.writeInt(30)
        buffer.writeBytes("my-name".toByteArray(CharsetUtil.UTF_8))

        val out = mutableListOf<Any>()
        this.decoder.decode(mockk(), buffer, out)
        assertThat(buffer.readerIndex()).isEqualTo(0)
    }

    @Test
    fun `"should correctly decode a message"`() {

        val buffer = Unpooled.buffer()
        val text = "This is an error message"
        val textBytes = text.toByteArray(CharsetUtil.UTF_8)

        buffer.writeByte('E'.toInt())
        buffer.writeInt(textBytes.length + 4 + 1 + 1)
        buffer.writeByte('M'.toInt())
        buffer.writeBytes(textBytes)
        buffer.writeByte(0)
        val out = mutableListOf<Any>()
        this.decoder.decode(mockk(), buffer, out)
        assertThat(out.size).isEqualTo(1)
        val result = out.get(0) as ErrorMessage
        assertThat(result.message).isEqualTo(text)
        assertThat(buffer.readerIndex()).isEqualTo((textBytes.length + 4 + 1 + 1 + 1))
    }

    @Test(expected = NegativeMessageSizeException::class)
    fun `"should raise an exception if the length is negative"`() {
        val buffer = Unpooled.buffer()
        buffer.writeByte(ServerMessage.Close)
        buffer.writeInt(2)
        val out = mutableListOf<Any>()

        this.decoder.decode(mockk(), buffer, out)
    }

    @Test(expected = MessageTooLongException::class)
    fun `"should raise an exception if the length is too big"`() {

        val buffer = Unpooled.buffer()
        buffer.writeByte(ServerMessage.Close)
        buffer.writeInt(MessageDecoder_DefaultMaximumSize + 10)
        val out = mutableListOf<Any>()

        this.decoder.decode(mockk(), buffer, out)
    }

}


