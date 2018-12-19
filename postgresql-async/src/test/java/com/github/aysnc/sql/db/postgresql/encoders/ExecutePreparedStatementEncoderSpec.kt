package com.github.aysnc.sql.db.postgresql.encoders

import com.github.jasync.sql.db.postgresql.column.PostgreSQLColumnEncoderRegistry
import com.github.jasync.sql.db.postgresql.encoders.ExecutePreparedStatementEncoder
import com.github.jasync.sql.db.postgresql.messages.frontend.PreparedStatementExecuteMessage
import io.netty.util.CharsetUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test


class ExecutePreparedStatementEncoderSpec {

    val registry = PostgreSQLColumnEncoderRegistry()
    val encoder = ExecutePreparedStatementEncoder(CharsetUtil.UTF_8, registry)
    val sampleMessage = byteArrayOf(
        66,
        0,
        0,
        0,
        18,
        49,
        0,
        49,
        0,
        0,
        0,
        0,
        1,
        -1,
        -1,
        -1,
        -1,
        0,
        0,
        69,
        0,
        0,
        0,
        10,
        49,
        0,
        0,
        0,
        0,
        0,
        83,
        0,
        0,
        0,
        4,
        67,
        0,
        0,
        0,
        7,
        80,
        49,
        0
    )

    @Test
    fun `encoder should correctly handle the case where an encoder returns null`() {


        val message = PreparedStatementExecuteMessage(1, "select * from users", listOf(null), registry)

        val result = encoder.encode(message)

        val bytes = ByteArray(result.readableBytes()) { 0 }
        result.readBytes(bytes)

        assertThat(bytes).isEqualTo(sampleMessage)


    }

}
