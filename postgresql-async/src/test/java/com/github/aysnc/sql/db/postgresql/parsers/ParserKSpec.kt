package com.github.aysnc.sql.db.postgresql.parsers

import com.github.jasync.sql.db.postgresql.messages.backend.ProcessData
import com.github.jasync.sql.db.postgresql.messages.backend.ServerMessage
import com.github.jasync.sql.db.postgresql.parsers.BackendKeyDataParser
import io.netty.buffer.Unpooled
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test


class ParserKSpec {

    val parser = BackendKeyDataParser


    @Test
    fun `"parserk" should" correctly parse the message"`() {

        val buffer = Unpooled.buffer()
        buffer.writeInt(10)
        buffer.writeInt(20)

        val data = parser.parseMessage(buffer) as ProcessData

        assertThat(data.kind).isEqualTo(ServerMessage.BackendKeyData)
        assertThat(data.processId).isEqualTo(10)
        assertThat(data.secretKey).isEqualTo(20)

    }

}

