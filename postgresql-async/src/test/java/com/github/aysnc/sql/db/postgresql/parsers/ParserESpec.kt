package com.github.aysnc.sql.db.postgresql.parsers

import com.github.jasync.sql.db.postgresql.messages.backend.ErrorMessage
import com.github.jasync.sql.db.postgresql.messages.backend.ServerMessage
import com.github.jasync.sql.db.postgresql.parsers.ErrorParser
import io.netty.buffer.Unpooled
import io.netty.util.CharsetUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ParserESpec {

    @Test
    fun `ErrorParser should correctly parse an error message`() {
        val content = "this is my error message"
        val error = content.toByteArray(CharsetUtil.UTF_8)
        val buffer = Unpooled.buffer()
        buffer.writeByte('M'.toInt())
        buffer.writeBytes(error)
        buffer.writeByte(0)

        val message = ErrorParser(CharsetUtil.UTF_8).parseMessage(buffer) as ErrorMessage

        assertThat(message.message).isEqualTo(content)
        assertThat(message.kind).isEqualTo(ServerMessage.Error)
    }
}
