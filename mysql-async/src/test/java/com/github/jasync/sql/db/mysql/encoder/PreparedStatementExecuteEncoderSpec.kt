package com.github.jasync.sql.db.mysql.encoder

import com.github.jasync.sql.db.mysql.binary.BinaryRowEncoder
import io.netty.util.CharsetUtil
import org.junit.Test
import kotlin.test.assertEquals

class PreparedStatementExecuteEncoderSpec {

    private val encoder = PreparedStatementExecuteEncoder(BinaryRowEncoder(CharsetUtil.UTF_8))

    @Test
    fun `encode Some(value) like value`() {
        val actual = encoder.encodeValues(listOf(1L, "foo"), setOf(0, 1))
        val expected = encoder.encodeValues(listOf(1L, "foo"), setOf(0, 1))

        assertEquals(expected, actual)

    }

    @Test
    fun `encode None as null`() {
        val actual = encoder.encodeValues(listOf(null), setOf(0))
        val expected = encoder.encodeValues(listOf(null), setOf(0))
        assertEquals(expected, actual)

    }

}
