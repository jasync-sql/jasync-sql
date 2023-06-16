package com.github.aysnc.sql.db.postgresql.parsers

import com.github.jasync.sql.db.postgresql.messages.backend.ParameterStatusMessage
import com.github.jasync.sql.db.postgresql.messages.backend.ServerMessage
import com.github.jasync.sql.db.postgresql.parsers.ParameterStatusParser
import io.netty.buffer.Unpooled
import io.netty.util.CharsetUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.nio.charset.Charset

class ParserSSpec {

    val parser = ParameterStatusParser(CharsetUtil.UTF_8)

    @Test
    fun `ParameterStatusParser should correctly parse a config pair`() {
        val key = "application-name"
        val value = "my-cool-application"

        val buffer = Unpooled.buffer()

        buffer.writeBytes(key.toByteArray(Charset.forName("UTF-8")))
        buffer.writeByte(0)
        buffer.writeBytes(value.toByteArray(Charset.forName("UTF-8")))
        buffer.writeByte(0)

        val content = this.parser.parseMessage(buffer) as ParameterStatusMessage

        assertThat(content.key).isEqualTo(key)
        assertThat(content.value).isEqualTo(value)
        assertThat(content.kind).isEqualTo(ServerMessage.ParameterStatus)
        assertThat(buffer.readerIndex()).isEqualTo(buffer.writerIndex())
    }
}
